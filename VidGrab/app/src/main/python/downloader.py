import json
import os
import traceback

import yt_dlp


def _progress_hook(callback):
    def hook(info):
        status = info.get("status")
        if status == "downloading":
            downloaded = info.get("downloaded_bytes", 0)
            total = info.get("total_bytes") or info.get("total_bytes_estimate", 0)
            percent = 0.0
            if total and total > 0:
                percent = min(100.0, downloaded / total * 100.0)
            speed = info.get("speed", 0) or 0
            eta = info.get("eta", 0) or 0
            try:
                callback.onProgress(
                    percent,
                    int(downloaded),
                    int(total),
                    int(speed),
                    int(eta),
                    info.get("filename", ""),
                )
            except Exception:
                traceback.print_exc()
        elif status == "finished":
            try:
                callback.onConverting(info.get("filename", ""))
            except Exception:
                traceback.print_exc()

    return hook


def download(url, out_dir, options=None, callback=None):
    """
    Download a video with yt-dlp.

    Args:
        url: The video URL.
        out_dir: Directory where files are written.
        options: Optional dict of yt-dlp options (e.g. format selection).
        callback: Optional object with onStart/onProgress/onConverting/onComplete/onError methods.

    Returns:
        dict: {"status": "ok" | "error", "file": str | None, "message": str}
    """
    if not url:
        return json.dumps(
            {"status": "error", "file": None, "message": "No URL provided"}
        )

    os.makedirs(out_dir, exist_ok=True)

    opts = {
        "outtmpl": os.path.join(out_dir, "%(title)s [%(id)s].%(ext)s"),
        "progress_hooks": [_progress_hook(callback)] if callback else [],
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
    }
    if options:
        opts.update(options)

    if callback is not None:
        try:
            callback.onStart(url)
        except Exception:
            traceback.print_exc()

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

            if callback is not None:
                try:
                    callback.onComplete(filename)
                except Exception:
                    traceback.print_exc()

            return json.dumps(
                {"status": "ok", "file": filename, "message": "Download complete"}
            )

    except yt_dlp.utils.DownloadError as e:
        if callback is not None:
            try:
                callback.onError(str(e))
            except Exception:
                traceback.print_exc()
        return json.dumps({"status": "error", "file": None, "message": str(e)})
    except Exception as e:
        traceback.print_exc()
        if callback is not None:
            try:
                callback.onError(str(e))
            except Exception:
                traceback.print_exc()
        return json.dumps({"status": "error", "file": None, "message": str(e)})


def get_version():
    """Return yt-dlp version for diagnostics."""
    return yt_dlp.version.__version__
