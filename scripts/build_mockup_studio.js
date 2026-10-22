const fs = require('fs');
const path = require('path');

const b64Data = JSON.parse(fs.readFileSync(path.join(__dirname, '../mockup_assets/images_base64.json'), 'utf8'));

const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Play Store Mockup Studio — Black Screen Recorder Pro</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg-dark: #0a0a0c;
      --bg-card: #141417;
      --bg-card-hover: #1c1c21;
      --border-subtle: #26262c;
      --accent-red: #e50914;
      --accent-red-hover: #f40612;
      --text-primary: #f5f5f7;
      --text-secondary: #9e9ea7;
      --text-muted: #62626b;
      --sidebar-width: 320px;
    }

    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
      background-color: var(--bg-dark);
      color: var(--text-primary);
      height: 100vh;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    header {
      height: 60px;
      background: #111114;
      border-bottom: 1px solid var(--border-subtle);
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 24px;
      flex-shrink: 0;
      z-index: 10;
    }

    .brand { display: flex; align-items: center; gap: 12px; }
    .brand-icon {
      width: 32px; height: 32px;
      background: linear-gradient(135deg, #e50914, #8b0000);
      border-radius: 8px;
      display: flex; align-items: center; justify-content: center;
      font-weight: 800; font-size: 14px;
      box-shadow: 0 0 12px rgba(229, 9, 20, 0.4);
    }
    .brand-title { font-weight: 700; font-size: 15px; letter-spacing: -0.2px; }
    .brand-badge {
      font-size: 11px; padding: 2px 8px; border-radius: 12px;
      background: rgba(229, 9, 20, 0.15); color: var(--accent-red);
      font-weight: 600; border: 1px solid rgba(229, 9, 20, 0.3);
    }

    .header-actions { display: flex; align-items: center; gap: 12px; }
    .btn {
      padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600;
      cursor: pointer; display: inline-flex; align-items: center; gap: 8px;
      transition: all 0.2s ease; border: 1px solid transparent; outline: none;
    }
    .btn-secondary { background: var(--bg-card); color: var(--text-primary); border-color: var(--border-subtle); }
    .btn-secondary:hover { background: var(--bg-card-hover); border-color: #383842; }
    .btn-primary { background: var(--accent-red); color: #fff; box-shadow: 0 2px 10px rgba(229, 9, 20, 0.35); }
    .btn-primary:hover { background: var(--accent-red-hover); transform: translateY(-1px); }

    .workspace { flex: 1; display: flex; overflow: hidden; }

    .slide-strip {
      width: 230px; background: #0d0d10; border-right: 1px solid var(--border-subtle);
      display: flex; flex-direction: column; overflow-y: auto; padding: 16px 12px;
      gap: 10px; flex-shrink: 0;
    }
    .strip-title {
      font-size: 11px; font-weight: 700; color: var(--text-muted);
      text-transform: uppercase; letter-spacing: 0.8px; padding: 0 4px 6px;
    }
    .slide-item {
      padding: 10px; background: var(--bg-card); border: 1px solid var(--border-subtle);
      border-radius: 10px; cursor: pointer; display: flex; flex-direction: column;
      gap: 4px; transition: all 0.2s;
    }
    .slide-item:hover { background: var(--bg-card-hover); border-color: #383842; }
    .slide-item.active {
      border-color: var(--accent-red); background: #1b1618;
      box-shadow: 0 0 14px rgba(229, 9, 20, 0.18);
    }
    .slide-item-num { font-size: 10px; font-weight: 700; color: var(--accent-red); text-transform: uppercase; }
    .slide-item-title { font-size: 12px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .preview-viewport {
      flex: 1; background: #08080a; display: flex; align-items: center;
      justify-content: center; overflow: auto; padding: 30px; position: relative;
    }
    .canvas-container {
      position: relative; border-radius: 16px; overflow: hidden;
      box-shadow: 0 25px 60px rgba(0, 0, 0, 0.8), 0 0 0 1px rgba(255, 255, 255, 0.05);
      background: #000;
    }
    #previewCanvas { display: block; max-height: calc(100vh - 140px); width: auto; border-radius: 16px; }

    .inspector {
      width: var(--sidebar-width); background: #111114; border-left: 1px solid var(--border-subtle);
      overflow-y: auto; display: flex; flex-direction: column; flex-shrink: 0;
    }
    .panel-section { padding: 18px 20px; border-bottom: 1px solid var(--border-subtle); }
    .panel-title {
      font-size: 11px; font-weight: 700; color: var(--text-muted);
      text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 14px;
      display: flex; align-items: center; justify-content: space-between;
    }
    .control-group { margin-bottom: 14px; }
    .control-group:last-child { margin-bottom: 0; }
    .control-label {
      font-size: 12px; font-weight: 600; color: var(--text-secondary);
      margin-bottom: 6px; display: flex; justify-content: space-between;
    }
    .control-input, .control-textarea {
      width: 100%; background: #18181c; border: 1px solid #2a2a32;
      border-radius: 8px; padding: 9px 12px; color: var(--text-primary);
      font-family: inherit; font-size: 13px; transition: border-color 0.2s;
    }
    .control-input:focus, .control-textarea:focus { border-color: var(--accent-red); outline: none; }
    .control-textarea { resize: vertical; min-height: 54px; line-height: 1.4; }

    .color-row { display: flex; gap: 10px; }
    .color-picker-box {
      flex: 1; display: flex; align-items: center; gap: 8px;
      background: #18181c; border: 1px solid #2a2a32; border-radius: 8px; padding: 6px 10px;
    }
    .color-picker-box input[type="color"] {
      -webkit-appearance: none; border: none; width: 22px; height: 22px;
      border-radius: 4px; cursor: pointer; background: none;
    }
    .color-picker-box span { font-family: 'JetBrains Mono', monospace; font-size: 11px; color: var(--text-secondary); }

    .file-upload-drop {
      border: 2px dashed #2e2e38; border-radius: 10px; padding: 16px;
      text-align: center; cursor: pointer; transition: all 0.2s;
      background: rgba(255, 255, 255, 0.01);
    }
    .file-upload-drop:hover { border-color: var(--accent-red); background: rgba(229, 9, 20, 0.05); }
    .upload-icon { font-size: 20px; margin-bottom: 4px; }
    .upload-text { font-size: 12px; font-weight: 600; color: var(--text-secondary); }
    .upload-hint { font-size: 11px; color: var(--text-muted); margin-top: 2px; }

    .range-wrap { display: flex; align-items: center; gap: 12px; }
    .range-input { flex: 1; accent-color: var(--accent-red); }
    .range-value { font-family: 'JetBrains Mono', monospace; font-size: 11px; color: var(--text-secondary); width: 38px; text-align: right; }

    .preset-pill-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 6px; }
    .preset-btn {
      padding: 8px 10px; background: #18181c; border: 1px solid #2a2a32;
      border-radius: 6px; color: var(--text-secondary); font-size: 11px;
      font-weight: 600; cursor: pointer; text-align: center; transition: all 0.2s;
    }
    .preset-btn:hover { background: var(--bg-card-hover); color: var(--text-primary); border-color: var(--accent-red); }
  </style>
</head>
<body>

  <header>
    <div class="brand">
      <div class="brand-icon">B</div>
      <div class="brand-title">Mockup Studio</div>
      <span class="brand-badge">Black Screen Recorder</span>
    </div>
    <div class="header-actions">
      <button class="btn btn-secondary" id="btnExportAll"><span>⬇️ Download All 5</span></button>
      <button class="btn btn-primary" id="btnExportCurrent"><span>📸 Download Slide (<span id="btnSlideNum">1</span>)</span></button>
    </div>
  </header>

  <div class="workspace">
    <div class="slide-strip" id="slideStrip">
      <div class="strip-title">Mockup Slides</div>
    </div>

    <div class="preview-viewport">
      <div class="canvas-container">
        <canvas id="previewCanvas" width="1080" height="1920"></canvas>
      </div>
    </div>

    <div class="inspector">
      <div class="panel-section">
        <div class="panel-title">Typography & Copy</div>
        <div class="control-group">
          <label class="control-label">Category Tag (Pill)</label>
          <input type="text" class="control-input" id="inpTag" placeholder="e.g. COVERS">
        </div>
        <div class="control-group">
          <label class="control-label">Headline Text</label>
          <textarea class="control-textarea" id="inpTitle" rows="2" placeholder="Headline text..."></textarea>
        </div>
        <div class="control-group">
          <label class="control-label">Highlighted Word (Red Accent)</label>
          <input type="text" class="control-input" id="inpHighlight" placeholder="e.g. lock screen">
        </div>
        <div class="control-group">
          <label class="control-label">Sub-headline Description</label>
          <textarea class="control-textarea" id="inpSubtitle" rows="2" placeholder="Sub-headline text..."></textarea>
        </div>
      </div>

      <div class="panel-section">
        <div class="panel-title">Phone Screen Image</div>
        <div class="file-upload-drop" id="dropArea">
          <input type="file" id="fileUpload" accept="image/*" style="display:none">
          <div class="upload-icon">📁</div>
          <div class="upload-text">Upload Custom Screenshot</div>
          <div class="upload-hint">Drag & drop or click (PNG/JPG)</div>
        </div>

        <div class="control-group" style="margin-top:12px;">
          <label class="control-label">App Screens & Presets</label>
          <div class="preset-pill-grid">
            <button class="preset-btn" data-preset="record">Record Tab</button>
            <button class="preset-btn" data-preset="lockscreen">Realistic Lockscreen</button>
            <button class="preset-btn" data-preset="covers">Covers Grid</button>
            <button class="preset-btn" data-preset="vault">Security PIN Vault</button>
            <button class="preset-btn" data-preset="player">Player List</button>
          </div>
        </div>
      </div>

      <div class="panel-section">
        <div class="panel-title">Background & Glow</div>
        <div class="control-group">
          <label class="control-label">Gradient Colors (Top & Bottom)</label>
          <div class="color-row">
            <div class="color-picker-box">
              <input type="color" id="inpBgTop" value="#161619">
              <span id="lblBgTop">#161619</span>
            </div>
            <div class="color-picker-box">
              <input type="color" id="inpBgBottom" value="#09090b">
              <span id="lblBgBottom">#09090B</span>
            </div>
          </div>
        </div>
        <div class="control-group">
          <label class="control-label">Ambient Red Glow Behind Phone</label>
          <div class="range-wrap">
            <input type="range" class="range-input" id="inpGlowOpacity" min="0" max="60" value="25">
            <span class="range-value" id="lblGlowOpacity">25%</span>
          </div>
        </div>
      </div>

      <div class="panel-section">
        <div class="panel-title">Phone Frame & Layout</div>
        <div class="control-group">
          <label class="control-label">Phone Scale</label>
          <div class="range-wrap">
            <input type="range" class="range-input" id="inpPhoneScale" min="70" max="115" value="100">
            <span class="range-value" id="lblPhoneScale">100%</span>
          </div>
        </div>
        <div class="control-group">
          <label class="control-label">Vertical Position (Y Offset)</label>
          <div class="range-wrap">
            <input type="range" class="range-input" id="inpPhoneY" min="-150" max="250" value="0">
            <span class="range-value" id="lblPhoneY">0px</span>
          </div>
        </div>
      </div>
    </div>
  </div>

  <script>
    const PRESET_IMAGES = {
      lockscreen: "${b64Data.lockscreen}",
      vault: "${b64Data.slide4}"
    };

    const slides = [
      {
        id: 1,
        tag: "RECORD",
        title: "Record with screen off",
        highlight: "off",
        subtitle: "Capture Full HD video & audio without turning on the display",
        presetType: "record",
        customImg: null,
        bgTop: "#161619",
        bgBottom: "#09090b",
        glow: 25,
        phoneScale: 100,
        phoneY: 0
      },
      {
        id: 2,
        tag: "COVERS",
        title: "Looks like a normal lock screen",
        highlight: "lock screen",
        subtitle: "Discreetly disguise active recordings as an everyday phone screen",
        presetType: "lockscreen",
        customImg: null,
        bgTop: "#171415",
        bgBottom: "#080708",
        glow: 30,
        phoneScale: 100,
        phoneY: 0
      },
      {
        id: 3,
        tag: "DISGUISE",
        title: "Multiple stealth covers",
        highlight: "stealth covers",
        subtitle: "Choose between digital clock, pure black, or custom disguises",
        presetType: "covers",
        customImg: null,
        bgTop: "#161619",
        bgBottom: "#09090b",
        glow: 22,
        phoneScale: 100,
        phoneY: 0
      },
      {
        id: 4,
        tag: "VAULT",
        title: "Keep your clips private",
        highlight: "private",
        subtitle: "Protect your recordings behind Fingerprint & PIN lock",
        presetType: "vault",
        customImg: null,
        bgTop: "#181314",
        bgBottom: "#0a0708",
        glow: 32,
        phoneScale: 100,
        phoneY: 0
      },
      {
        id: 5,
        tag: "GALLERY",
        title: "Built-in private player",
        highlight: "private player",
        subtitle: "Play, organize, rename, and export clips without cloud uploads",
        presetType: "player",
        customImg: null,
        bgTop: "#161619",
        bgBottom: "#09090b",
        glow: 24,
        phoneScale: 100,
        phoneY: 0
      }
    ];

    let currentSlideIndex = 0;
    const canvas = document.getElementById('previewCanvas');
    const ctx = canvas.getContext('2d');

    const imageCache = {};
    function getOrLoadImage(src, callback) {
      if (imageCache[src]) {
        callback(imageCache[src]);
        return;
      }
      const img = new Image();
      img.onload = () => {
        imageCache[src] = img;
        callback(img);
      };
      img.src = src;
    }

    function drawFallbackScreen(ctxX, x, y, w, h, type) {
      ctxX.save();
      ctxX.beginPath();
      ctxX.roundRect(x, y, w, h, 48);
      ctxX.clip();

      ctxX.fillStyle = '#0d0d0f';
      ctxX.fillRect(x, y, w, h);

      ctxX.fillStyle = '#ffffff';
      ctxX.font = '600 22px "Plus Jakarta Sans"';
      ctxX.fillText('10:14', x + 40, y + 54);
      ctxX.font = '600 18px "Plus Jakarta Sans"';
      ctxX.fillStyle = '#9e9ea7';
      ctxX.textAlign = 'right';
      ctxX.fillText('5G  📶  100%', x + w - 40, y + 54);
      ctxX.textAlign = 'left';

      if (type === 'record') {
        ctxX.fillStyle = '#ffffff';
        ctxX.font = '700 32px "Plus Jakarta Sans"';
        ctxX.fillText('BLACK SCREEN RECORDER', x + 40, y + 130);

        ctxX.fillStyle = '#18181c';
        ctxX.beginPath();
        ctxX.roundRect(x + 36, y + 165, w - 72, 90, 16);
        ctxX.fill();
        ctxX.strokeStyle = '#2b2b35';
        ctxX.stroke();

        ctxX.fillStyle = '#9e9ea7';
        ctxX.font = '500 20px "Plus Jakarta Sans"';
        ctxX.fillText('Preset', x + 60, y + 218);
        ctxX.fillStyle = '#ffffff';
        ctxX.font = '700 22px "Plus Jakarta Sans"';
        ctxX.fillText('Discreet Video ›', x + w - 240, y + 218);

        ctxX.fillStyle = '#18181c';
        ctxX.beginPath();
        ctxX.roundRect(x + 36, y + 280, w - 72, 70, 16);
        ctxX.fill();

        ctxX.fillStyle = '#e50914';
        ctxX.beginPath();
        ctxX.roundRect(x + 42, y + 286, (w - 84)/2, 58, 12);
        ctxX.fill();
        ctxX.fillStyle = '#ffffff';
        ctxX.font = '700 22px "Plus Jakarta Sans"';
        ctxX.textAlign = 'center';
        ctxX.fillText('▶ Video', x + 42 + (w - 84)/4, y + 322);
        ctxX.fillStyle = '#9e9ea7';
        ctxX.fillText('🎙 Audio', x + 42 + (w - 84)*0.75, y + 322);
        ctxX.textAlign = 'left';

        ctxX.fillStyle = '#18181c';
        ctxX.beginPath();
        ctxX.roundRect(x + 36, y + 375, (w - 92)/2, 76, 14);
        ctxX.roundRect(x + 56 + (w - 92)/2, y + 375, (w - 92)/2, 76, 14);
        ctxX.fill();

        ctxX.fillStyle = '#ffffff';
        ctxX.font = '600 20px "Plus Jakarta Sans"';
        ctxX.fillText('📷 Back Camera', x + 56, y + 421);
        ctxX.fillText('⚙️ 1080p Full HD', x + 76 + (w - 92)/2, y + 421);

        const btnY = y + 500;
        const grad = ctxX.createLinearGradient(x + 36, btnY, x + w - 36, btnY);
        grad.addColorStop(0, '#e50914');
        grad.addColorStop(1, '#c00811');
        ctxX.fillStyle = grad;
        ctxX.beginPath();
        ctxX.roundRect(x + 36, btnY, w - 72, 90, 45);
        ctxX.fill();
        ctxX.shadowColor = 'rgba(229, 9, 20, 0.45)';
        ctxX.shadowBlur = 25;
        ctxX.fillStyle = '#ffffff';
        ctxX.font = '800 24px "Plus Jakarta Sans"';
        ctxX.textAlign = 'center';
        ctxX.fillText('●  START RECORDING', x + w/2, btnY + 55);
        ctxX.shadowBlur = 0;
        ctxX.textAlign = 'left';

        ctxX.fillStyle = '#141418';
        ctxX.beginPath();
        ctxX.roundRect(x + 36, y + 640, w - 72, 180, 20);
        ctxX.fill();
        ctxX.strokeStyle = '#282832';
        ctxX.stroke();

        ctxX.fillStyle = '#e50914';
        ctxX.font = '700 16px "Plus Jakarta Sans"';
        ctxX.fillText('ACTIVE COVER', x + 64, y + 685);
        ctxX.fillStyle = '#ffffff';
        ctxX.font = '700 24px "Plus Jakarta Sans"';
        ctxX.fillText('Discreet Clock Display', x + 64, y + 725);
        ctxX.fillStyle = '#7a7a85';
        ctxX.font = '500 18px "Plus Jakarta Sans"';
        ctxX.fillText('Shows Always-on Clock during recording', x + 64, y + 765);

      } else if (type === 'covers') {
        ctxX.fillStyle = '#ffffff';
        ctxX.font = '700 32px "Plus Jakarta Sans"';
        ctxX.fillText('RECORDING COVERS', x + 40, y + 130);

        const cardW = (w - 92) / 2;
        const cardH = 220;
        const covers = [
          { name: 'Pure Black', desc: '0% Battery OLED', active: true },
          { name: 'Digital Clock', desc: 'Minimalist AOD', active: false },
          { name: 'Analog Clock', desc: 'Classic Dial', active: false },
          { name: 'Phone Call', desc: 'Incoming Screen', active: false }
        ];

        covers.forEach((c, idx) => {
          const cx = x + 36 + (idx % 2) * (cardW + 20);
          const cy = y + 180 + Math.floor(idx / 2) * (cardH + 20);

          ctxX.fillStyle = c.active ? '#1c1417' : '#141418';
          ctxX.beginPath();
          ctxX.roundRect(cx, cy, cardW, cardH, 18);
          ctxX.fill();
          ctxX.strokeStyle = c.active ? '#e50914' : '#282832';
          ctxX.lineWidth = c.active ? 2.5 : 1;
          ctxX.stroke();

          ctxX.fillStyle = '#08080a';
          ctxX.beginPath();
          ctxX.roundRect(cx + 16, cy + 16, cardW - 32, 110, 10);
          ctxX.fill();

          if (idx === 0) {
            ctxX.fillStyle = '#333333';
            ctxX.beginPath();
            ctxX.roundRect(cx + cardW/2 - 25, cy + 28, 50, 4, 2);
            ctxX.fill();
          } else if (idx === 1) {
            ctxX.fillStyle = '#ffffff';
            ctxX.font = '700 24px "JetBrains Mono"';
            ctxX.textAlign = 'center';
            ctxX.fillText('10:14', cx + cardW/2, cy + 80);
            ctxX.textAlign = 'left';
          }

          ctxX.fillStyle = '#ffffff';
          ctxX.font = '700 20px "Plus Jakarta Sans"';
          ctxX.fillText(c.name, cx + 18, cy + 165);
          ctxX.fillStyle = '#7a7a85';
          ctxX.font = '500 16px "Plus Jakarta Sans"';
          ctxX.fillText(c.desc, cx + 18, cy + 195);
        });

      } else if (type === 'player') {
        ctxX.fillStyle = '#ffffff';
        ctxX.font = '700 32px "Plus Jakarta Sans"';
        ctxX.fillText('RECORDINGS', x + 40, y + 130);

        ['All (12)', 'Videos (8)', 'Audio (4)'].forEach((tab, i) => {
          ctxX.fillStyle = i === 0 ? '#e50914' : '#18181c';
          ctxX.beginPath();
          ctxX.roundRect(x + 40 + i * 140, y + 165, 125, 46, 23);
          ctxX.fill();
          ctxX.fillStyle = '#ffffff';
          ctxX.font = '600 17px "Plus Jakarta Sans"';
          ctxX.textAlign = 'center';
          ctxX.fillText(tab, x + 40 + i * 140 + 62, y + 194);
        });
        ctxX.textAlign = 'left';

        for (let i = 0; i < 4; i++) {
          const itemY = y + 240 + i * 125;
          ctxX.fillStyle = '#141418';
          ctxX.beginPath();
          ctxX.roundRect(x + 36, itemY, w - 72, 105, 16);
          ctxX.fill();
          ctxX.strokeStyle = '#22222a';
          ctxX.stroke();

          ctxX.fillStyle = '#22222e';
          ctxX.beginPath();
          ctxX.roundRect(x + 50, itemY + 14, 110, 77, 10);
          ctxX.fill();
          ctxX.fillStyle = '#e50914';
          ctxX.font = '700 18px "Plus Jakarta Sans"';
          ctxX.fillText('▶ 04:18', x + 62, itemY + 58);

          ctxX.fillStyle = '#ffffff';
          ctxX.font = '600 20px "Plus Jakarta Sans"';
          ctxX.fillText('Recording_' + (104 - i) + '.mp4', x + 180, itemY + 45);
          ctxX.fillStyle = '#7a7a85';
          ctxX.font = '500 16px "Plus Jakarta Sans"';
          ctxX.fillText('1080p Full HD  •  48 MB  •  Today', x + 180, itemY + 76);
        }
      }

      ctxX.restore();
    }

    function render() {
      const slide = slides[currentSlideIndex];
      const W = canvas.width;
      const H = canvas.height;

      const bgGrad = ctx.createLinearGradient(0, 0, 0, H);
      bgGrad.addColorStop(0, slide.bgTop);
      bgGrad.addColorStop(1, slide.bgBottom);
      ctx.fillStyle = bgGrad;
      ctx.fillRect(0, 0, W, H);

      if (slide.glow > 0) {
        const glowOpacity = slide.glow / 100;
        const radial = ctx.createRadialGradient(W / 2, 1150 + slide.phoneY, 100, W / 2, 1150 + slide.phoneY, 650);
        radial.addColorStop(0, \`rgba(229, 9, 20, \${glowOpacity})\`);
        radial.addColorStop(0.6, \`rgba(229, 9, 20, \${glowOpacity * 0.3})\`);
        radial.addColorStop(1, 'rgba(0, 0, 0, 0)');
        ctx.fillStyle = radial;
        ctx.fillRect(0, 450, W, H - 450);
      }

      if (slide.tag) {
        ctx.save();
        ctx.textAlign = 'center';
        ctx.fillStyle = '#e50914';
        ctx.font = '800 22px "Plus Jakarta Sans"';
        ctx.letterSpacing = '2px';
        ctx.fillText(slide.tag.toUpperCase(), W / 2, 160);
        ctx.restore();
      }

      ctx.save();
      ctx.textAlign = 'center';
      const headlineY = 245;
      const title = slide.title || '';
      const highlight = (slide.highlight || '').trim().toLowerCase();

      ctx.font = '800 68px "Plus Jakarta Sans"';
      ctx.letterSpacing = '-1px';

      if (!highlight || !title.toLowerCase().includes(highlight)) {
        ctx.fillStyle = '#ffffff';
        ctx.fillText(title, W / 2, headlineY);
      } else {
        const lowerTitle = title.toLowerCase();
        const startIdx = lowerTitle.indexOf(highlight);
        const before = title.substring(0, startIdx);
        const match = title.substring(startIdx, startIdx + highlight.length);
        const after = title.substring(startIdx + highlight.length);

        const wBefore = ctx.measureText(before).width;
        const wMatch = ctx.measureText(match).width;
        const wAfter = ctx.measureText(after).width;
        const totalW = wBefore + wMatch + wAfter;

        let curX = (W / 2) - (totalW / 2);

        ctx.textAlign = 'left';
        ctx.fillStyle = '#ffffff';
        ctx.fillText(before, curX, headlineY);
        curX += wBefore;

        ctx.fillStyle = '#e50914';
        ctx.fillText(match, curX, headlineY);
        curX += wMatch;

        ctx.fillStyle = '#ffffff';
        ctx.fillText(after, curX, headlineY);
      }
      ctx.restore();

      if (slide.subtitle) {
        ctx.save();
        ctx.textAlign = 'center';
        ctx.fillStyle = '#9e9ea7';
        ctx.font = '500 28px "Plus Jakarta Sans"';
        ctx.fillText(slide.subtitle, W / 2, 320);
        ctx.restore();
      }

      const basePhoneW = 760;
      const basePhoneH = 1540;
      const scale = slide.phoneScale / 100;
      const phoneW = basePhoneW * scale;
      const phoneH = basePhoneH * scale;
      const phoneX = (W - phoneW) / 2;
      const phoneY = 410 + slide.phoneY;

      ctx.save();
      ctx.shadowColor = 'rgba(0, 0, 0, 0.75)';
      ctx.shadowBlur = 60;
      ctx.shadowOffsetY = 30;
      ctx.fillStyle = '#0a0a0c';
      ctx.beginPath();
      ctx.roundRect(phoneX, phoneY, phoneW, phoneH, 68 * scale);
      ctx.fill();
      ctx.restore();

      ctx.save();
      const bezelGrad = ctx.createLinearGradient(phoneX, phoneY, phoneX + phoneW, phoneY + phoneH);
      bezelGrad.addColorStop(0, '#2e2e34');
      bezelGrad.addColorStop(0.5, '#18181c');
      bezelGrad.addColorStop(1, '#2a2a30');
      ctx.fillStyle = bezelGrad;
      ctx.beginPath();
      ctx.roundRect(phoneX, phoneY, phoneW, phoneH, 68 * scale);
      ctx.fill();

      ctx.strokeStyle = '#3c3c46';
      ctx.lineWidth = 2 * scale;
      ctx.stroke();

      const bezelPadding = 16 * scale;
      const screenX = phoneX + bezelPadding;
      const screenY = phoneY + bezelPadding;
      const screenW = phoneW - (bezelPadding * 2);
      const screenH = phoneH - (bezelPadding * 2);

      const renderScreenContent = (loadedImg) => {
        ctx.save();
        ctx.beginPath();
        ctx.roundRect(screenX, screenY, screenW, screenH, 52 * scale);
        ctx.clip();

        if (loadedImg) {
          ctx.drawImage(loadedImg, screenX, screenY, screenW, screenH);
        } else {
          drawFallbackScreen(ctx, screenX, screenY, screenW, screenH, slide.presetType);
        }

        ctx.fillStyle = '#000000';
        ctx.beginPath();
        ctx.arc(screenX + screenW / 2, screenY + (36 * scale), 14 * scale, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#222222';
        ctx.lineWidth = 1.5;
        ctx.stroke();

        ctx.restore();
      };

      if (slide.customImg) {
        getOrLoadImage(slide.customImg, renderScreenContent);
      } else if (PRESET_IMAGES[slide.presetType]) {
        getOrLoadImage(PRESET_IMAGES[slide.presetType], renderScreenContent);
      } else {
        renderScreenContent(null);
      }

      ctx.restore();
    }

    function syncInspector() {
      const slide = slides[currentSlideIndex];
      document.getElementById('btnSlideNum').innerText = slide.id;
      document.getElementById('inpTag').value = slide.tag;
      document.getElementById('inpTitle').value = slide.title;
      document.getElementById('inpHighlight').value = slide.highlight;
      document.getElementById('inpSubtitle').value = slide.subtitle;
      document.getElementById('inpBgTop').value = slide.bgTop;
      document.getElementById('lblBgTop').innerText = slide.bgTop.toUpperCase();
      document.getElementById('inpBgBottom').value = slide.bgBottom;
      document.getElementById('lblBgBottom').innerText = slide.bgBottom.toUpperCase();
      document.getElementById('inpGlowOpacity').value = slide.glow;
      document.getElementById('lblGlowOpacity').innerText = slide.glow + '%';
      document.getElementById('inpPhoneScale').value = slide.phoneScale;
      document.getElementById('lblPhoneScale').innerText = slide.phoneScale + '%';
      document.getElementById('inpPhoneY').value = slide.phoneY;
      document.getElementById('lblPhoneY').innerText = slide.phoneY + 'px';
      render();
    }

    function renderSlideStrip() {
      const strip = document.getElementById('slideStrip');
      strip.innerHTML = '<div class="strip-title">Mockup Slides</div>';
      slides.forEach((s, idx) => {
        const item = document.createElement('div');
        item.className = 'slide-item' + (idx === currentSlideIndex ? ' active' : '');
        item.innerHTML = \`
          <div class="slide-item-num">Slide \${s.id} • \${s.tag}</div>
          <div class="slide-item-title">\${s.title}</div>
        \`;
        item.onclick = () => {
          currentSlideIndex = idx;
          renderSlideStrip();
          syncInspector();
        };
        strip.appendChild(item);
      });
    }

    document.getElementById('inpTag').oninput = (e) => { slides[currentSlideIndex].tag = e.target.value; render(); renderSlideStrip(); };
    document.getElementById('inpTitle').oninput = (e) => { slides[currentSlideIndex].title = e.target.value; render(); renderSlideStrip(); };
    document.getElementById('inpHighlight').oninput = (e) => { slides[currentSlideIndex].highlight = e.target.value; render(); };
    document.getElementById('inpSubtitle').oninput = (e) => { slides[currentSlideIndex].subtitle = e.target.value; render(); };
    document.getElementById('inpBgTop').oninput = (e) => {
      slides[currentSlideIndex].bgTop = e.target.value;
      document.getElementById('lblBgTop').innerText = e.target.value.toUpperCase();
      render();
    };
    document.getElementById('inpBgBottom').oninput = (e) => {
      slides[currentSlideIndex].bgBottom = e.target.value;
      document.getElementById('lblBgBottom').innerText = e.target.value.toUpperCase();
      render();
    };
    document.getElementById('inpGlowOpacity').oninput = (e) => {
      slides[currentSlideIndex].glow = parseInt(e.target.value);
      document.getElementById('lblGlowOpacity').innerText = e.target.value + '%';
      render();
    };
    document.getElementById('inpPhoneScale').oninput = (e) => {
      slides[currentSlideIndex].phoneScale = parseInt(e.target.value);
      document.getElementById('lblPhoneScale').innerText = e.target.value + '%';
      render();
    };
    document.getElementById('inpPhoneY').oninput = (e) => {
      slides[currentSlideIndex].phoneY = parseInt(e.target.value);
      document.getElementById('lblPhoneY').innerText = e.target.value + 'px';
      render();
    };

    document.querySelectorAll('.preset-btn').forEach(btn => {
      btn.onclick = () => {
        const type = btn.getAttribute('data-preset');
        slides[currentSlideIndex].presetType = type;
        slides[currentSlideIndex].customImg = null;
        render();
      };
    });

    const dropArea = document.getElementById('dropArea');
    const fileUpload = document.getElementById('fileUpload');
    dropArea.onclick = () => fileUpload.click();
    fileUpload.onchange = (e) => handleFiles(e.target.files);
    dropArea.ondragover = (e) => { e.preventDefault(); dropArea.style.borderColor = '#e50914'; };
    dropArea.ondragleave = () => { dropArea.style.borderColor = '#2e2e38'; };
    dropArea.ondrop = (e) => {
      e.preventDefault();
      dropArea.style.borderColor = '#2e2e38';
      handleFiles(e.dataTransfer.files);
    };

    function handleFiles(files) {
      if (files && files[0]) {
        const reader = new FileReader();
        reader.onload = (event) => {
          slides[currentSlideIndex].customImg = event.target.result;
          render();
        };
        reader.readAsDataURL(files[0]);
      }
    }

    function downloadCanvas(name) {
      const link = document.createElement('a');
      link.download = name + '.png';
      link.href = canvas.toDataURL('image/png', 1.0);
      link.click();
    }

    document.getElementById('btnExportCurrent').onclick = () => {
      const s = slides[currentSlideIndex];
      downloadCanvas('playstore_mockup_slide' + s.id + '_' + s.tag.toLowerCase());
    };

    document.getElementById('btnExportAll').onclick = async () => {
      const origIndex = currentSlideIndex;
      for (let i = 0; i < slides.length; i++) {
        currentSlideIndex = i;
        syncInspector();
        await new Promise(r => setTimeout(r, 200));
        downloadCanvas('playstore_mockup_slide' + (i + 1) + '_' + slides[i].tag.toLowerCase());
      }
      currentSlideIndex = origIndex;
      syncInspector();
    };

    renderSlideStrip();
    syncInspector();
  </script>
</body>
</html>`;

const outDir = path.join(__dirname, '../mockup_studio');
fs.writeFileSync(path.join(outDir, 'index.html'), htmlContent, 'utf8');
console.log('Mockup studio built successfully!');
