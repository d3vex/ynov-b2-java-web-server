#!/usr/bin/env python3
import sys
import os

method = os.environ.get("REQUEST_METHOD", "GET")
content_length = os.environ.get("CONTENT_LENGTH", "0")

body = sys.stdin.read(int(content_length)) if content_length.isdigit() and int(content_length) > 0 else ""

print("Content-Type: text/html")
print()
print("<!DOCTYPE html>")
print("<html><head><title>Echo CGI</title></head><body>")
print("<h1>Echo CGI</h1>")
print("<pre>")
print("Method: " + method)
print("Body: " + body)
print("</pre>")
print("</body></html>")
