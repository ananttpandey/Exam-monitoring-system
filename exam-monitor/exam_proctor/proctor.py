import cv2
import mediapipe as mp
import time
import sys

# ─── MediaPipe Hands Setup ────────────────────────────────────────────────────
mp_hands = mp.solutions.hands
mp_drawing = mp.solutions.drawing_utils
hands_detector = mp_hands.Hands(
    static_image_mode=False,
    max_num_hands=2,
    min_detection_confidence=0.7,
    min_tracking_confidence=0.5
)

# ─── Haarcascade Setup ────────────────────────────────────────────────────────
face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
eye_cascade  = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_eye.xml")

# ─── State Variables ─────────────────────────────────────────────────────────
warning_counter       = 0
eyes_missing_since    = None   # timestamp when eyes were last NOT detected
tab_switch_counter    = 0
NO_EYE_THRESHOLD      = 3.0   # seconds before warning triggers

# ─── Colors (BGR) ────────────────────────────────────────────────────────────
GREEN  = (0, 255, 0)
RED    = (0, 0, 255)
YELLOW = (0, 220, 255)
WHITE  = (255, 255, 255)
BLACK  = (0, 0, 0)
CYAN   = (255, 200, 0)

# ─── Helper: Draw semi-transparent overlay text ───────────────────────────────
def draw_label(frame, text, pos, color, font_scale=0.65, thickness=2):
    x, y = pos
    (tw, th), _ = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, font_scale, thickness)
    cv2.rectangle(frame, (x - 4, y - th - 6), (x + tw + 4, y + 6), BLACK, -1)
    cv2.putText(frame, text, (x, y), cv2.FONT_HERSHEY_SIMPLEX, font_scale, color, thickness)

# ─── Helper: Draw warning banner ─────────────────────────────────────────────
def draw_banner(frame, text, color):
    h, w = frame.shape[:2]
    overlay = frame.copy()
    cv2.rectangle(overlay, (0, h - 55), (w, h), color, -1)
    cv2.addWeighted(overlay, 0.55, frame, 0.45, 0, frame)
    (tw, _), _ = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, 0.8, 2)
    cv2.putText(frame, text, ((w - tw) // 2, h - 18),
                cv2.FONT_HERSHEY_SIMPLEX, 0.8, WHITE, 2)

# ─── Webcam ───────────────────────────────────────────────────────────────────
cap = cv2.VideoCapture(0)
if not cap.isOpened():
    print("[ERROR] Cannot open webcam.")
    sys.exit(1)

print("[INFO] Proctor started. Press 'Q' to quit.")
print("[INFO] Press 'T' to simulate a tab switch.")

# ─── Main Loop ────────────────────────────────────────────────────────────────
while True:
    ret, frame = cap.read()
    if not ret:
        break

    frame = cv2.flip(frame, 1)
    h, w = frame.shape[:2]
    gray  = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    rgb   = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    now   = time.time()

    active_warnings = []

    # ── 1. Face + Eye Detection ───────────────────────────────────────────────
    faces     = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(80, 80))
    eyes_found = False

    for (fx, fy, fw, fh) in faces:
        cv2.rectangle(frame, (fx, fy), (fx + fw, fy + fh), GREEN, 2)
        draw_label(frame, "Face", (fx, fy - 8), GREEN)

        face_roi_gray  = gray[fy:fy + fh, fx:fx + fw]
        face_roi_color = frame[fy:fy + fh, fx:fx + fw]

        eyes = eye_cascade.detectMultiScale(face_roi_gray, scaleFactor=1.05,
                                            minNeighbors=6, minSize=(20, 20))
        for (ex, ey, ew, eh) in eyes:
            cv2.rectangle(face_roi_color, (ex, ey), (ex + ew, ey + eh), CYAN, 2)
            eyes_found = True

    # ── 2. Eye-absence timer & warning ───────────────────────────────────────
    if not eyes_found:
        if eyes_missing_since is None:
            eyes_missing_since = now
        elapsed = now - eyes_missing_since
        if elapsed >= NO_EYE_THRESHOLD:
            warning_counter += 1
            eyes_missing_since = now          # reset so it doesn't keep firing
            active_warnings.append(f"NOT LOOKING AT SCREEN  (Warning #{warning_counter})")
        else:
            # Show countdown before warning fires
            remaining = NO_EYE_THRESHOLD - elapsed
            draw_label(frame, f"Eyes missing: {remaining:.1f}s", (10, h - 70), YELLOW)
    else:
        eyes_missing_since = None             # reset when eyes come back

    # ── 3. Hand Detection ─────────────────────────────────────────────────────
    result = hands_detector.process(rgb)
    hand_detected = False

    if result.multi_hand_landmarks:
        hand_detected = True
        for hand_lm in result.multi_hand_landmarks:
            mp_drawing.draw_landmarks(frame, hand_lm, mp_hands.HAND_CONNECTIONS,
                mp_drawing.DrawingSpec(color=(0, 255, 180), thickness=2, circle_radius=3),
                mp_drawing.DrawingSpec(color=(255, 100, 0), thickness=2))
        active_warnings.append("HAND DETECTED")

    # ── 4. HUD — top-left status panel ───────────────────────────────────────
    status_lines = [
        (f"Faces detected : {len(faces)}",        GREEN  if len(faces) > 0 else RED),
        (f"Eyes detected  : {'Yes' if eyes_found else 'No'}", GREEN if eyes_found else RED),
        (f"Hand detected  : {'Yes' if hand_detected else 'No'}", RED if hand_detected else GREEN),
        (f"Warning count  : {warning_counter}",   YELLOW if warning_counter > 0 else WHITE),
        (f"Tab switches   : {tab_switch_counter}", YELLOW if tab_switch_counter > 0 else WHITE),
    ]
    panel_x, panel_y = 10, 15
    for i, (txt, col) in enumerate(status_lines):
        draw_label(frame, txt, (panel_x, panel_y + i * 26), col, font_scale=0.58)

    # ── 5. Warning banners at bottom ─────────────────────────────────────────
    if active_warnings:
        draw_banner(frame, " | ".join(active_warnings), RED)

    # ── 6. Title bar ─────────────────────────────────────────────────────────
    cv2.rectangle(frame, (0, 0), (w, 12), (30, 30, 30), -1)
    cv2.putText(frame, "EXAM PROCTOR  |  Q=Quit  T=Simulate Tab Switch",
                (6, 10), cv2.FONT_HERSHEY_PLAIN, 0.85, (180, 180, 180), 1)

    cv2.imshow("Exam Proctoring System", frame)

    # ── 7. Key handling ───────────────────────────────────────────────────────
    key = cv2.waitKey(1) & 0xFF
    if key == ord('q') or key == ord('Q'):
        break
    elif key == ord('t') or key == ord('T'):
        tab_switch_counter += 1
        warning_counter    += 1
        print(f"[ALERT] Tab switch simulated! Total: {tab_switch_counter}")

# ─── Cleanup ──────────────────────────────────────────────────────────────────
cap.release()
cv2.destroyAllWindows()

print("\n─── SESSION SUMMARY ───────────────────────")
print(f"  Total warnings    : {warning_counter}")
print(f"  Tab switches      : {tab_switch_counter}")
print("───────────────────────────────────────────")
