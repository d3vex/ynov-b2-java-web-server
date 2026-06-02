#!/usr/bin/env python3
import os

print("Content-Type: text/html")
print()
print("<!DOCTYPE html>")
print("<html><head><title>CGI Test</title></head><body>")
print("<h1>CGI Hello from Python!</h1>")
print("<pre>")
print("REQUEST_METHOD: " + os.environ.get("REQUEST_METHOD", "unknown"))
print("QUERY_STRING: " + os.environ.get("QUERY_STRING", ""))
print("SCRIPT_NAME: " + os.environ.get("SCRIPT_NAME", ""))
print("PATH_INFO: " + os.environ.get("PATH_INFO", ""))
print("SERVER_PORT: " + os.environ.get("SERVER_PORT", ""))
print("</pre>")
print("</body></html>")
