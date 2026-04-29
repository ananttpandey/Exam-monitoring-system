# Exam Proctoring System

Real-time webcam-based exam monitoring using OpenCV + MediaPipe.

## Setup

```bash
pip install -r requirements.txt
python proctor.py
```

## Controls

| Key | Action |
|-----|--------|
| `Q` | Quit |
| `T` | Simulate tab switch |

## What it detects

| Event | Trigger | Action |
|-------|---------|--------|
| Not looking | Eyes absent > 3 seconds | Warning banner + counter++ |
| Hand detected | Any hand in frame | Warning banner |
| Tab switch | Press `T` key | Counter++ + warning |

## Session Summary
Printed to terminal on exit.
