#!/usr/bin/env python3
import time

# Simulate slow CGI (2 seconds)
time.sleep(2)

print("Content-Type: text/html")
print()
print("<!DOCTYPE html>")
print("<html><head><title>Slow CGI</title></head><body>")
print("<h1>Slow CGI Response</h1>")
print("<p>Slept for 2 seconds but didn't block other requests!</p>")
print("</body></html>")
