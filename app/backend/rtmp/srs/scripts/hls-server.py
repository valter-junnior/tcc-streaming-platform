#!/usr/bin/env python3
"""
Minimal HTTP server for serving HLS files with CORS headers.
Replaces `python3 -m http.server` which does not support CORS.
"""
import sys
from http.server import SimpleHTTPRequestHandler, HTTPServer

CORS_HEADERS = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, HEAD, OPTIONS",
    "Access-Control-Allow-Headers": "Origin, Range, Accept-Encoding, Referer, User-Agent",
    "Access-Control-Expose-Headers": "Content-Length, Content-Range",
}


class CORSRequestHandler(SimpleHTTPRequestHandler):
    def end_headers(self):
        for key, value in CORS_HEADERS.items():
            self.send_header(key, value)
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Max-Age", "3600")
        self.end_headers()

    def log_message(self, fmt, *args):
        # Suppress .ts segment access logs to reduce noise; keep playlists and errors.
        if args and isinstance(args[0], str) and args[0].endswith(".ts"):
            return
        super().log_message(fmt, *args)


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8081
    directory = sys.argv[2] if len(sys.argv) > 2 else os.getcwd()

    handler = lambda *args, **kwargs: CORSRequestHandler(*args, directory=directory, **kwargs)
    server = HTTPServer(("", port), handler)
    print(f"HLS HTTP server with CORS started on port {port}, serving from {directory}", flush=True)
    server.serve_forever()
