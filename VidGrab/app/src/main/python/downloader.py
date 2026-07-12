import json
import logging
import os
import traceback
from urllib.parse import parse_qsl, urlparse, urlencode, urlunparse

import yt_dlp


logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("downloader")


ALLOWED_SCHEMES = {"http", "https"}

# Query parameters that are safe to strip before passing a share URL to yt-dlp.
_STRIP_PARAMS = {"igsh", "igshid"}


def _safe_call(callback, method_name, *args, **kwargs):
    """Invoke a callback method, logging any exception instead of swallowing it."""
    if callback is None:
        return
    try:
        getattr(callback, method_name)(*args, **kwargs)
    except Exception:
        logger.exception("Callback %s failed", method_name)
        traceback.print_exc()


def _progress_hook(callback):
    def hook(info):
        if callback is not None:
            try:
                if callback.isCancelled():
                    raise yt_dlp.utils.DownloadError("Download cancelled")
            except Exception:
                traceback.print_exc()

        status = info.get("status")
        if status == "downloading":
            downloaded = info.get("downloaded_bytes", 0)
            total = info.get("total_bytes") or info.get("total_bytes_estimate", 0)
            percent = 0.0
            if total and total > 0:
                percent = min(100.0, downloaded / total * 100.0)
            speed = info.get("speed", 0) or 0
            eta = info.get("eta", 0) or 0
            _safe_call(
                callback,
                "onProgress",
                percent,
                int(downloaded),
                int(total),
                int(speed),
                int(eta),
                info.get("filename", ""),
            )
        elif status == "finished":
            _safe_call(callback, "onConverting", info.get("filename", ""))

    return hook


def _is_valid_url(url):
    parsed = urlparse(url)
    return parsed.scheme in ALLOWED_SCHEMES and parsed.netloc


def _sanitize_url(url):
    """Remove Instagram share/tracking parameters that can confuse yt-dlp."""
    parsed = urlparse(url)
    if not parsed.query:
        return url
    pairs = parse_qsl(parsed.query, keep_blank_values=True)
    filtered = [
        (k, v)
        for k, v in pairs
        if k.lower() not in _STRIP_PARAMS and not k.lower().startswith("utm_")
    ]
    if len(filtered) == len(pairs):
        return url
    return urlunparse(parsed._replace(query=urlencode(filtered)))


def download(url, out_dir, cookiefile=None, user_agent=None, callback=None):
    """
    Download a video with yt-dlp.

    Args:
        url: The video URL.
        out_dir: Directory where files are written.
        cookiefile: Optional path to a Netscape-format cookie file.
        user_agent: Optional user agent string to use for HTTP requests.
        callback: Optional object with onStart/onProgress/onConverting/onComplete/onError methods.

    Returns:
        dict: {"status": "ok" | "error", "file": str | None, "message": str}
    """
    if not url:
        return json.dumps(
            {"status": "error", "file": None, "message": "No URL provided"}
        )

    url = _sanitize_url(url)

    if not _is_valid_url(url):
        return json.dumps({"status": "error", "file": None, "message": "Invalid URL"})

    os.makedirs(out_dir, exist_ok=True)

    opts = {
        "outtmpl": os.path.join(out_dir, "%(title).80B [%(id)s].%(ext)s"),
        "progress_hooks": [_progress_hook(callback)] if callback else [],
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
    }
    if user_agent:
        opts["user_agent"] = user_agent
    if cookiefile:
        logger.info(
            "Using cookie file: %s (exists=%s)", cookiefile, os.path.exists(cookiefile)
        )
        opts["cookiefile"] = cookiefile

    _safe_call(callback, "onStart", url)

    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=True)
            if info is None:
                return json.dumps(
                    {
                        "status": "error",
                        "file": None,
                        "message": "Could not extract video info",
                    }
                )

            filename = ydl.prepare_filename(info)
            # prepare_filename reflects the final extension; if post-processing changed it,
            # the actual file may differ. Try to locate the real file.
            if not os.path.exists(filename):
                base = os.path.splitext(filename)[0]
                for ext in (".mp4", ".webm", ".mkv", ".mov"):
                    candidate = base + ext
                    if os.path.exists(candidate):
                        filename = candidate
                        break

            _safe_call(callback, "onComplete", filename)

            return json.dumps(
                {"status": "ok", "file": filename, "message": "Download complete"}
            )

    except yt_dlp.utils.DownloadError as e:
        _safe_call(callback, "onError", str(e))
        return json.dumps({"status": "error", "file": None, "message": str(e)})
    except Exception as e:
        logger.exception("Unexpected error during download")
        _safe_call(callback, "onError", str(e))
        return json.dumps({"status": "error", "file": None, "message": str(e)})


def get_version():
    """Return yt-dlp version for diagnostics."""
    return yt_dlp.version.__version__
