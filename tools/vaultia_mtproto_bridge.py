"""
Vaultia MTProto Streaming Bridge
================================
A lightweight high-performance Telegram streaming bridge for Vaultia.
Enables instant zero-buffering playback of large video files (> 20MB up to 2GB)
forwarded or shared directly to your Telegram bot.

Inspired by TelePlay's MTProto parallel streaming engine.

Requirements:
    pip install aiohttp pyrogram tgcrypto

Usage:
    python vaultia_mtproto_bridge.py --api-id <YOUR_API_ID> --api-hash <YOUR_API_HASH> --port 8081
"""

import argparse
import asyncio
import logging
import os
import re
from aiohttp import web, ClientSession
from pyrogram import Client
from pyrogram.file_id import FileId

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("VaultiaBridge")

# Cache for active Pyrogram bot clients keyed by bot token
clients = {}
file_cache = {}

async def get_or_create_client(api_id: int, api_hash: str, bot_token: str) -> Client:
    if bot_token in clients:
        return clients[bot_token]

    session_name = f"vaultia_bot_{bot_token[:10]}"
    app = Client(
        name=session_name,
        api_id=api_id,
        api_hash=api_hash,
        bot_token=bot_token,
        in_memory=True
    )
    await app.start()
    clients[bot_token] = app
    logger.info(f"Initialized MTProto client for bot {bot_token[:10]}...")
    return app


async def handle_get_file(request):
    token = request.match_info["token"]
    file_id = request.query.get("file_id")

    if not file_id:
        return web.json_response({"ok": False, "error_code": 400, "description": "Missing file_id"})

    api_id = request.app["api_id"]
    api_hash = request.app["api_hash"]

    try:
        # Decode file_id to obtain file attributes
        parsed = FileId.decode(file_id)
        file_size = parsed.file_size or 0
        file_cache[file_id] = {"size": file_size, "token": token}

        return web.json_response({
            "ok": True,
            "result": {
                "file_id": file_id,
                "file_size": file_size,
                "file_path": f"stream/{file_id}"
            }
        })
    except Exception as e:
        logger.warning(f"Failed to parse file_id via MTProto: {e}. Falling back to api.telegram.org...")
        # Fallback to upstream Telegram Bot API
        upstream_url = f"https://api.telegram.org/bot{token}/getFile?file_id={file_id}"
        async with ClientSession() as session:
            async with session.get(upstream_url) as upstream_res:
                data = await upstream_res.json()
                return web.json_response(data, status=upstream_res.status)


async def handle_stream(request):
    token = request.match_info["token"]
    file_id = request.match_info["file_id"]

    api_id = request.app["api_id"]
    api_hash = request.app["api_hash"]

    try:
        client = await get_or_create_client(api_id, api_hash, token)
    except Exception as e:
        logger.error(f"Failed to start Pyrogram client: {e}")
        return web.Response(status=500, text=f"MTProto client error: {e}")

    try:
        parsed = FileId.decode(file_id)
        total_size = parsed.file_size or 0

        range_header = request.headers.get("Range")
        start = 0
        end = total_size - 1 if total_size > 0 else 0

        if range_header:
            match = re.match(r"bytes=(\d+)-(\d*)", range_header)
            if match:
                start = int(match.group(1))
                if match.group(2):
                    end = int(match.group(2))

        if total_size > 0:
            start = max(0, min(start, total_size - 1))
            end = max(start, min(end, total_size - 1))

        content_length = (end - start + 1) if total_size > 0 else 0

        headers = {
            "Content-Type": "video/mp4",
            "Accept-Ranges": "bytes",
            "Connection": "keep-alive"
        }
        status = 200

        if range_header and total_size > 0:
            status = 206
            headers["Content-Range"] = f"bytes {start}-{end}/{total_size}"
            headers["Content-Length"] = str(content_length)
        elif total_size > 0:
            headers["Content-Length"] = str(total_size)

        response = web.StreamResponse(status=status, headers=headers)
        await response.prepare(request)

        # Stream requested range in 1MB chunks using Pyrogram MTProto
        chunk_size = 1024 * 1024  # 1MB chunks (matching TelePlay)
        offset = start
        bytes_to_send = content_length if total_size > 0 else 100 * 1024 * 1024

        async for chunk in client.stream_media(file_id, offset=offset // chunk_size, limit=bytes_to_send // chunk_size + 2):
            if not chunk:
                break
            await response.write(chunk)

        await response.write_eof()
        return response

    except Exception as e:
        logger.error(f"Error streaming file {file_id}: {e}")
        return web.Response(status=500, text=f"Streaming error: {e}")


async def handle_proxy_fallback(request):
    """Transparently proxies all other Bot API requests to https://api.telegram.org"""
    path = request.path
    query = request.query_string
    upstream_url = f"https://api.telegram.org{path}"
    if query:
        upstream_url += f"?{query}"

    async with ClientSession() as session:
        headers = {k: v for k, v in request.headers.items() if k.lower() not in ("host", "content-length")}
        data = await request.read()
        async with session.request(request.method, upstream_url, headers=headers, data=data) as resp:
            body = await resp.read()
            resp_headers = {k: v for k, v in resp.headers.items() if k.lower() not in ("content-encoding", "transfer-encoding")}
            return web.Response(status=resp.status, body=body, headers=resp_headers)


def main():
    parser = argparse.ArgumentParser(description="Vaultia MTProto Streaming Bridge")
    parser.add_argument("--api-id", type=int, default=int(os.environ.get("TELEGRAM_API_ID", "0")), help="Telegram API ID from my.telegram.org")
    parser.add_argument("--api-hash", type=str, default=os.environ.get("TELEGRAM_API_HASH", ""), help="Telegram API Hash from my.telegram.org")
    parser.add_argument("--port", type=int, default=8081, help="Port to run bridge on (default: 8081)")
    parser.add_argument("--host", type=str, default="0.0.0.0", help="Host address (default: 0.0.0.0)")

    args = parser.parse_args()

    app = web.Application()
    app["api_id"] = args.api_id
    app["api_hash"] = args.api_hash

    app.router.add_get("/bot{token}/getFile", handle_get_file)
    app.router.add_get("/file/bot{token}/stream/{file_id}", handle_stream)
    app.router.add_route("*", "/{path:.*}", handle_proxy_fallback)

    print("=" * 60)
    print("  🚀 Vaultia MTProto Streaming Bridge")
    print(f"  Listening on: http://{args.host}:{args.port}")
    print(f"  Telegram API ID: {args.api_id or 'Not set'}")
    print("  Zero-buffering large file streaming active!")
    print("=" * 60)

    web.run_app(app, host=args.host, port=args.port)


if __name__ == "__main__":
    main()
