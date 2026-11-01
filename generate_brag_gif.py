import os
import math
from PIL import Image, ImageDraw, ImageFont

WIDTH = 800
HEIGHT = 450
TOTAL_FRAMES = 200  # 20 seconds at 10 FPS
FPS = 10
FRAME_DURATION = 100  # ms

# Load fonts
FONT_REGULAR = "C:/Windows/Fonts/segoeui.ttf"
FONT_BOLD = "C:/Windows/Fonts/segoeuib.ttf"
FONT_MONO = "C:/Windows/Fonts/consola.ttf"
FONT_MONO_BOLD = "C:/Windows/Fonts/consolab.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except:
        return ImageFont.load_default()

font_h1 = get_font(FONT_BOLD, 30)
font_h2 = get_font(FONT_BOLD, 22)
font_h3 = get_font(FONT_BOLD, 17)
font_body = get_font(FONT_REGULAR, 15)
font_small = get_font(FONT_REGULAR, 12)
font_mono = get_font(FONT_MONO, 13)
font_mono_bold = get_font(FONT_MONO_BOLD, 13)
font_mono_small = get_font(FONT_MONO, 11)
font_score_big = get_font(FONT_BOLD, 46)

# Colors
BG_DARK = (11, 19, 43)        # Deep navy/slate console
BG_DARKER = (7, 12, 28)
CARD_BG = (19, 31, 66)
BORDER_COLOR = (42, 60, 112)
TEXT_WHITE = (255, 255, 255)
TEXT_SLATE = (148, 163, 184)
TEXT_MUTED = (100, 116, 139)
ACCENT_BLUE = (59, 130, 246)
ACCENT_GREEN = (16, 185, 129)
ACCENT_AMBER = (245, 158, 11)
ACCENT_RED = (239, 68, 68)
ACCENT_INDIGO = (99, 102, 241)
ACCENT_GOLD = (234, 179, 8)

# Load Logo
logo_path = "e:/Elenchos/Logo.png"
logo_raw = None
if os.path.exists(logo_path):
    logo_raw = Image.open(logo_path).convert("RGBA")

def draw_header(draw, title_tag="ELENCHOS LABORATORY"):
    draw.rectangle([(0, 0), (WIDTH, 36)], fill=(15, 23, 42))
    draw.line([(0, 36), (WIDTH, 36)], fill=BORDER_COLOR, width=1)
    
    # macOS/Cockpit dots
    draw.ellipse([(14, 13), (24, 23)], fill=(239, 68, 68))
    draw.ellipse([(30, 13), (40, 23)], fill=(245, 158, 11))
    draw.ellipse([(46, 13), (56, 23)], fill=(16, 185, 129))
    
    # Title
    draw.text((70, 10), title_tag, font=font_mono_bold, fill=TEXT_SLATE)
    
    # Right pill
    pill_text = "AUTO QA v1.0 • 100% OFFLINE"
    draw.text((WIDTH - 210, 10), pill_text, font=font_mono_small, fill=ACCENT_GREEN)

def draw_progress_bar(draw, percent, text, y=422):
    draw.rectangle([(0, y), (WIDTH, HEIGHT)], fill=(15, 23, 42))
    draw.line([(0, y), (WIDTH, y)], fill=BORDER_COLOR, width=1)
    fill_w = int((WIDTH * (percent / 100.0)))
    draw.rectangle([(0, y + 24), (fill_w, HEIGHT)], fill=ACCENT_BLUE)
    draw.text((14, y + 5), text, font=font_mono_small, fill=TEXT_SLATE)
    pct_str = f"{int(percent)}%"
    draw.text((WIDTH - 45, y + 5), pct_str, font=font_mono_bold, fill=ACCENT_BLUE)

frames = []

for frame_idx in range(TOTAL_FRAMES):
    im = Image.new("RGBA", (WIDTH, HEIGHT), BG_DARK)
    draw = ImageDraw.Draw(im)
    
    # Scene 1: The Hook & Identity (Frames 0 - 35, 0.0s - 3.5s)
    if frame_idx < 35:
        draw_header(draw, "ELENCHOS • AUTONOMOUS MOBILE LAB")
        
        if frame_idx < 14:
            # Hook text typing / reveal
            t = frame_idx / 14.0
            draw.text((60, 120), "Android QA is broken.", font=font_h1, fill=ACCENT_RED)
            draw.text((60, 175), "Manual monkey-testing takes days.", font=font_h2, fill=TEXT_WHITE)
            draw.text((60, 215), "Emulators freeze. Fatal crashes slip into production.", font=font_body, fill=TEXT_SLATE)
            
            # Sub box
            draw.rounded_rectangle([(60, 280), (740, 350)], radius=8, fill=CARD_BG, outline=BORDER_COLOR)
            draw.text((80, 298), "What if an autonomous on-device laboratory audited everything in 60s?", font=font_h3, fill=ACCENT_BLUE)
            draw.text((80, 323), "Static security • 16KB Page alignment • Live UI exploration • AI Fix Prompts", font=font_small, fill=TEXT_SLATE)
            
            draw_progress_bar(draw, 5 + (frame_idx * 1.5), "INITIALIZING LABORATORY HARNESS...")
        else:
            # Reveal Logo + Elenchos Title
            f_rel = frame_idx - 14
            # Draw logo
            if logo_raw:
                target_sz = 120
                logo_resized = logo_raw.resize((target_sz, target_sz), Image.Resampling.LANCZOS)
                im.paste(logo_resized, (70, 90), logo_resized)
            
            draw.text((220, 105), "ELENCHOS (ἔλεγχος)", font=font_h1, fill=TEXT_WHITE)
            draw.text((220, 150), "God-Tier Android Autonomous Mobile QA Laboratory", font=font_h3, fill=ACCENT_BLUE)
            draw.text((220, 185), "Native on-device APK intelligence, boundary fuzzer & crash harvester", font=font_body, fill=TEXT_SLATE)
            
            # Badge row
            badges = [
                ("100/100 AUDIT GRADE", ACCENT_GREEN),
                ("14 DIAGNOSTIC PHASES", ACCENT_INDIGO),
                ("16KB ALIGNMENT VERIFIED", ACCENT_AMBER),
                ("ZERO CLOUD EXFILTRATION", ACCENT_BLUE)
            ]
            bx = 220
            for btext, bcol in badges:
                draw.rounded_rectangle([(bx, 230), (bx + 120, 258)], radius=4, fill=(15, 23, 42), outline=bcol)
                draw.text((bx + 8, 237), btext, font=get_font(FONT_BOLD, 9), fill=bcol)
                bx += 130
                
            draw.rounded_rectangle([(220, 285), (740, 355)], radius=6, fill=CARD_BG, outline=BORDER_COLOR)
            draw.text((235, 298), "\"Upload APK → Test Everything → Find Problems → Prove Them → Let AI Fix Them.\"", font=font_small, fill=ACCENT_GOLD)
            draw.text((235, 325), "Autonomous dual-engine: Accessibility Live Crawling + Deterministic Sandbox", font=font_mono_small, fill=TEXT_SLATE)
            
            draw_progress_bar(draw, 20 + (f_rel * 2.0), "LOADING LABORATORY SUBSYSTEMS...")

    # Scene 2: APK Ingestion & Deep Static Intelligence (Frames 35 - 75, 3.5s - 7.5s)
    elif frame_idx < 75:
        draw_header(draw, "PHASE 01-04 • APK DEEP INGESTION & BINARY AUDIT")
        
        f_rel = frame_idx - 35
        # Left Panel: APK Metadata HUD
        draw.rounded_rectangle([(30, 50), (380, 400)], radius=8, fill=CARD_BG, outline=BORDER_COLOR)
        draw.text((45, 65), "APK INGESTION INTELLIGENCE", font=font_mono_bold, fill=ACCENT_BLUE)
        
        rows = [
            ("Artifact Name", "target-sample-v2.4.apk"),
            ("Package ID", "com.example.targetapp"),
            ("Target / Min SDK", "API 35 / API 24"),
            ("File Size / Dex", "42.8 MB (3 Multidex)"),
            ("SHA-256 Digest", "9f8a...3b21 [VERIFIED]"),
            ("Signing Cert", "v2+v3 RSA-4096 Valid"),
            ("Components", "14 Activities • 4 Services"),
            ("Permissions", "28 Declared (6 Dangerous)")
        ]
        
        ry = 100
        for label, val in rows:
            draw.text((45, ry), label, font=font_mono_small, fill=TEXT_MUTED)
            draw.text((180, ry), val, font=font_mono_bold, fill=TEXT_WHITE)
            ry += 32
            
        # Right Panel: Security & 16KB Alignment Radar
        draw.rounded_rectangle([(410, 50), (770, 400)], radius=8, fill=CARD_BG, outline=BORDER_COLOR)
        draw.text((425, 65), "BINARY SECURITY & ABI RADAR", font=font_mono_bold, fill=ACCENT_AMBER)
        
        checks = [
            ("[PASS]", ACCENT_GREEN, "Android 15+ 16KB Page Alignment", "PT_LOAD segments 16384-byte aligned"),
            ("[PASS]", ACCENT_GREEN, "DEX Credential Scanner", "No unencrypted AWS / Google API keys"),
            ("[WARN]", ACCENT_AMBER, "P2: Cleartext Traffic Permitted", "usesCleartextTraffic='true' in manifest"),
            ("[CRIT]", ACCENT_RED, "P0: android:debuggable='true'", "Process memory attachable via ADB"),
            ("[WARN]", ACCENT_AMBER, "P1: Exported ContentProvider", "Provider exposed without permissions")
        ]
        
        cy = 100
        for i, (tag, col, title, sub) in enumerate(checks):
            # sequential reveal
            if f_rel > (i * 6):
                draw.rounded_rectangle([(425, cy), (755, cy + 50)], radius=4, fill=(15, 23, 42), outline=col)
                draw.text((435, cy + 8), tag, font=font_mono_bold, fill=col)
                draw.text((495, cy + 8), title, font=font_mono_bold, fill=TEXT_WHITE)
                draw.text((495, cy + 28), sub, font=font_mono_small, fill=TEXT_SLATE)
            cy += 58
            
        draw_progress_bar(draw, 40 + (f_rel * 0.8), "RUNNING BINARY AUDIT & CRYPTOGRAPHIC VERIFICATION...")

    # Scene 3: Live Autonomous UI Crawling & Input Fuzzing (Frames 75 - 120, 7.5s - 12.0s)
    elif frame_idx < 120:
        draw_header(draw, "PHASE 06-11 • AUTONOMOUS UI ENGINE & BOUNDARY FUZZER")
        f_rel = frame_idx - 75
        
        # Terminal Window
        draw.rounded_rectangle([(30, 50), (770, 400)], radius=8, fill=(7, 12, 28), outline=ACCENT_BLUE)
        draw.rectangle([(30, 50), (770, 80)], fill=(15, 23, 42))
        draw.text((45, 58), "ELENCHOS AUTONOMOUS COCKPIT • STREAMING LOGCAT & ACCESSIBILITY DISPATCH", font=font_mono_bold, fill=ACCENT_GREEN)
        
        logs = [
            ("[INFO]", ACCENT_BLUE, "ElenchosLabAccessibilityService: Active window tree attached"),
            ("[DISCOVERY]", ACCENT_INDIGO, "Discovered 6 screen nodes, 18 navigation paths in task stack"),
            ("[EXPLORE]", ACCENT_GREEN, "Live Window: Harvested 32 interactive UI elements"),
            ("[EXPLORE]", ACCENT_GREEN, "Auto-tap: Button 'btn_submit' (bounds: [120, 450, 680, 530])"),
            ("[FUZZ]", ACCENT_AMBER, "Injecting boundary vector: ' OR '1'='1' -- [SQL Mock]"),
            ("[FUZZ]", ACCENT_AMBER, "Injecting Unicode overflow: 🔥🚀🤖 (2000 multi-byte codepoints)"),
            ("[FUZZ]", ACCENT_AMBER, "Injecting boundary numeric: 999999999999999999 (Long Overflow)"),
            ("[NAV]", ACCENT_INDIGO, "Testing Back-Stack Stress: GLOBAL_ACTION_BACK x3 (0 loops detected)"),
            ("[LIFECYCLE]", ACCENT_BLUE, "Activity state audited: 9 exported components across 7 scenarios"),
            ("[LOGCAT]", ACCENT_GREEN, "Logcat Monitor: 0 unhandled runtime fatal crashes harvested")
        ]
        
        max_visible = 9
        start_log = max(0, int(f_rel / 3.5) - 4)
        visible_logs = logs[start_log : start_log + max_visible]
        
        ly = 95
        for tag, col, msg in visible_logs:
            draw.text((45, ly), tag, font=font_mono_bold, fill=col)
            draw.text((150, ly), msg, font=font_mono, fill=TEXT_WHITE)
            ly += 30
            
        draw_progress_bar(draw, 65 + (f_rel * 0.6), "LIVE CRAWLING ACTIVE • TESTING SCREEN HIERARCHIES...")

    # Scene 4: Health Score & AI Fix Handoff (Frames 120 - 165, 12.0s - 16.5s)
    elif frame_idx < 165:
        draw_header(draw, "PHASE 14 • HEALTH SCORE & AI FIX PACKAGE SYNTHESIS")
        f_rel = frame_idx - 120
        
        # Left: Health Score Card
        draw.rounded_rectangle([(30, 50), (360, 400)], radius=8, fill=CARD_BG, outline=BORDER_COLOR)
        draw.text((45, 65), "LABORATORY HEALTH SCORE", font=font_mono_bold, fill=TEXT_SLATE)
        
        # Big circular gauge representation
        score_val = min(88, int(f_rel * 3.5))
        draw.ellipse([(120, 100), (270, 250)], outline=BORDER_COLOR, width=12)
        # Arc
        draw.arc([(120, 100), (270, 250)], start=-90, end=-90 + int(score_val * 3.6), fill=ACCENT_GREEN, width=12)
        score_str = str(score_val)
        draw.text((165, 150), score_str, font=font_score_big, fill=TEXT_WHITE)
        draw.text((170, 205), "/ 100", font=font_h3, fill=TEXT_SLATE)
        
        # Subscore row
        draw.text((45, 275), "Stability: 95%   •   Security: 80%", font=font_mono_small, fill=TEXT_WHITE)
        draw.text((45, 305), "Function: 90%    •   Performance: 85%", font=font_mono_small, fill=TEXT_WHITE)
        draw.text((45, 335), "Compatibility: 100% (16KB Aligned)", font=font_mono_small, fill=ACCENT_GREEN)
        
        # Right: AI Fix Package Handoff
        draw.rounded_rectangle([(390, 50), (770, 400)], radius=8, fill=CARD_BG, outline=ACCENT_BLUE)
        draw.text((410, 65), "AI AGENT FIX PACKAGE SYNTHESIZER", font=font_mono_bold, fill=ACCENT_BLUE)
        
        draw.text((410, 95), "Deterministic JSON schema + Senior Engineer Prompt", font=font_small, fill=TEXT_SLATE)
        
        # Code snippet box
        draw.rounded_rectangle([(410, 120), (750, 290)], radius=6, fill=(7, 12, 28), outline=BORDER_COLOR)
        code_lines = [
            "{\n",
            "  \"project\": \"com.example.targetapp\",\n",
            "  \"healthScore\": { \"overall\": 88 },\n",
            "  \"issues\": [\n",
            "    { \"id\": \"SEC-DBG\", \"severity\": \"P0\",\n",
            "      \"fix\": \"Set android:debuggable='false'\" },\n",
            "    { \"id\": \"SEC-EXP\", \"severity\": \"P1\",\n",
            "      \"fix\": \"Add signature permission guard\" }\n",
            "  ]\n",
            "}"
        ]
        cy = 130
        for cl in code_lines:
            draw.text((420, cy), cl.strip(), font=font_mono_small, fill=ACCENT_GREEN if "fix" in cl else TEXT_SLATE)
            cy += 16
            
        # Glowing Copy Button
        button_clicked = f_rel > 22
        btn_bg = ACCENT_GREEN if button_clicked else ACCENT_BLUE
        draw.rounded_rectangle([(410, 310), (750, 355)], radius=6, fill=btn_bg)
        btn_text = "✓ COPIED TO CLIPBOARD (READY FOR CLAUDE / GPT / GEMINI)" if button_clicked else "⚡ COPY AI FIX PROMPT (ONE-CLICK HANDOFF)"
        draw.text((425, 325), btn_text, font=font_mono_bold, fill=TEXT_WHITE)
        
        draw_progress_bar(draw, 85 + (f_rel * 0.3), "SYNTHESIZING AGENT HANDOFF PROMPT...")

    # Scene 5: Outro & The Socratic Punchline (Frames 165 - 200, 16.5s - 20.0s)
    else:
        draw_header(draw, "ELENCHOS (ἔλεγχος) • READY FOR DEPLOYMENT")
        f_rel = frame_idx - 165
        
        # Logo in center
        if logo_raw:
            target_sz = 100
            logo_resized = logo_raw.resize((target_sz, target_sz), Image.Resampling.LANCZOS)
            im.paste(logo_resized, (350, 50), logo_resized)
            
        draw.text((250, 160), "ELENCHOS (ἔλεγχος)", font=font_h1, fill=TEXT_WHITE)
        draw.text((150, 205), "\"Test Everything Possible. Find Problems. Prove Them. Let AI Fix Them.\"", font=font_h3, fill=ACCENT_GOLD)
        
        # 4 Pillar cards
        pillars = [
            ("100% Offline", "Zero data exfiltration"),
            ("16KB Aligned", "Android 15+ ready"),
            ("14 Phase Engine", "Autonomous UI & fuzzing"),
            ("AI Fix Package", "One-click agent handoff")
        ]
        px = 60
        for title, sub in pillars:
            draw.rounded_rectangle([(px, 250), (px + 155, 310)], radius=6, fill=CARD_BG, outline=BORDER_COLOR)
            draw.text((px + 12, 262), title, font=font_mono_bold, fill=ACCENT_BLUE)
            draw.text((px + 12, 285), sub, font=font_mono_small, fill=TEXT_SLATE)
            px += 175
            
        # Install command bar
        draw.rounded_rectangle([(160, 335), (640, 385)], radius=6, fill=(15, 23, 42), outline=ACCENT_GREEN)
        draw.text((180, 350), "$ adb install -r elenchos-v1.0-release.apk", font=font_mono_bold, fill=ACCENT_GREEN)
        draw.text((580, 350), "[READY]", font=font_mono_bold, fill=TEXT_WHITE)
        
        draw_progress_bar(draw, 100, "LABORATORY AUDIT COMPLETE • 100/100 PRODUCTION GRADE")

    # Quantize to 256 colors for clean palette and compact GIF size
    frames.append(im.convert("RGB").quantize(colors=128, method=Image.Resampling.LANCZOS))

print(f"Generated {len(frames)} frames. Compiling animated GIF...")
gif_path = "e:/Elenchos/elenchos-preview.gif"
frames[0].save(
    gif_path,
    save_all=True,
    append_images=frames[1:],
    duration=FRAME_DURATION,
    loop=0,
    optimize=True
)

# Also copy to brag-output/
brag_gif = "e:/Elenchos/brag-output/elenchos-preview.gif"
import shutil
shutil.copyfile(gif_path, brag_gif)

file_sz = os.path.getsize(gif_path)
print(f"Successfully created {gif_path} ({file_sz / 1024 / 1024:.2f} MB)")
