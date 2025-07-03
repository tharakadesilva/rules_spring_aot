"""Mirror of release info for Spring AOT tools"""

# The integrity hashes can be computed with
# shasum -b -a 384 [downloaded file] | awk '{ print $1 }' | xxd -r -p | base64
TOOL_VERSIONS = {
    "3.4.7": {
        "x86_64-apple-darwin": "sha384-bPHNBJFjYhg5h+xguK51ycNt5IXMJ78f8JKNCcuMIiCOTMSMxn+bVDS6GxoKL34J",
        "aarch64-apple-darwin": "sha384-bPHNBJFjYhg5h+xguK51ycNt5IXMJ78f8JKNCcuMIiCOTMSMxn+bVDS6GxoKL34J",
        "x86_64-pc-windows-msvc": "sha384-bPHNBJFjYhg5h+xguK51ycNt5IXMJ78f8JKNCcuMIiCOTMSMxn+bVDS6GxoKL34J",
        "x86_64-unknown-linux-gnu": "sha384-bPHNBJFjYhg5h+xguK51ycNt5IXMJ78f8JKNCcuMIiCOTMSMxn+bVDS6GxoKL34J",
    },
}
