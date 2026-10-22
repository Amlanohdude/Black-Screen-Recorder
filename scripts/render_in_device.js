const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const edgePath = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const mockupAssetsDir = path.join(__dirname, '../mockup_assets');

const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=1440, height=2560, initial-scale=1.0">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@600;700;800&family=JetBrains+Mono:wght@600&display=swap" rel="stylesheet">
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      width: 1440px;
      height: 2560px;
      overflow: hidden;
      background: radial-gradient(circle at 50% 45%, #1c1518 0%, #0d0a0d 45%, #050507 100%);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      position: relative;
      font-family: 'Plus Jakarta Sans', sans-serif;
    }

    /* AMBIENT BACKLIGHT GLOW */
    .ambient-glow {
      position: absolute;
      width: 1000px;
      height: 1000px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(229, 9, 20, 0.32) 0%, rgba(229, 9, 20, 0.08) 50%, transparent 70%);
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      z-index: 1;
      pointer-events: none;
    }

    /* SMARTPHONE CHASSIS */
    .device-chassis {
      position: relative;
      width: 880px;
      height: 1820px;
      background: linear-gradient(135deg, #2d2d35 0%, #17171d 50%, #292932 100%);
      border-radius: 76px;
      padding: 16px;
      box-shadow:
        0 50px 120px rgba(0, 0, 0, 0.95),
        0 20px 40px rgba(0, 0, 0, 0.7),
        0 0 0 2px rgba(255, 255, 255, 0.12),
        inset 0 1px 3px rgba(255, 255, 255, 0.25);
      z-index: 10;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    /* INNER METALLIC BEZEL */
    .inner-bezel {
      width: 100%;
      height: 100%;
      background: #000;
      border-radius: 60px;
      overflow: hidden;
      position: relative;
      box-shadow: inset 0 0 0 3px #111116;
    }

    /* FRONT CAMERA PUNCH HOLE */
    .punch-hole {
      position: absolute;
      top: 26px;
      left: 50%;
      transform: translateX(-50%);
      width: 26px;
      height: 26px;
      background: #000;
      border-radius: 50%;
      border: 2px solid #1a1a20;
      box-shadow: inset 0 0 4px rgba(0,0,0,0.8), 0 0 2px rgba(255,255,255,0.1);
      z-index: 40;
    }
    .lens-glare {
      position: absolute;
      top: 5px;
      left: 5px;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.4);
    }

    /* SPEAKER SLIT */
    .speaker-slit {
      position: absolute;
      top: 10px;
      left: 50%;
      transform: translateX(-50%);
      width: 64px;
      height: 4px;
      background: #25252d;
      border-radius: 2px;
      z-index: 40;
    }

    /* SCREEN IMAGE FILL */
    .screen-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    /* SUBTLE GLASS REFLECTION */
    .glass-reflection {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 55%;
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.05) 0%, rgba(255, 255, 255, 0.01) 40%, transparent 60%);
      pointer-events: none;
      z-index: 30;
      border-radius: 60px 60px 0 0;
    }

    /* LABEL AT BOTTOM */
    .bottom-label {
      position: absolute;
      bottom: 120px;
      font-size: 26px;
      font-weight: 700;
      letter-spacing: 1.5px;
      color: #8c8c9a;
      text-transform: uppercase;
      z-index: 20;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .badge-live {
      background: rgba(229, 9, 20, 0.2);
      color: #e50914;
      font-size: 16px;
      font-weight: 800;
      padding: 4px 12px;
      border-radius: 8px;
      border: 1px solid rgba(229, 9, 20, 0.4);
    }
  </style>
</head>
<body>

  <div class="ambient-glow"></div>

  <!-- PHONE CONTAINER -->
  <div class="device-chassis">
    <div class="inner-bezel">
      <div class="speaker-slit"></div>
      <div class="punch-hole"><div class="lens-glare"></div></div>
      <div class="glass-reflection"></div>
      <img src="slide4_locking_vault_screen.png" class="screen-img" alt="In-App Lock Screen">
    </div>
  </div>

  <div class="bottom-label">
    In-App Vault Lock Screen <span class="badge-live">LIVE PREVIEW</span>
  </div>

</body>
</html>`;

const tempHtml = path.join(mockupAssetsDir, 'device_preview_temp.html');
fs.writeFileSync(tempHtml, htmlContent, 'utf8');

const outDevicePng = path.join(mockupAssetsDir, 'lockscreen_in_device.png');
const cmd = '"' + edgePath + '" --headless --disable-gpu --window-size=1440,2560 --screenshot="' + outDevicePng + '" "file:///' + tempHtml.replace(/\\/g, '/') + '"';

console.log('Rendering Lock Screen inside realistic device frame...');
execSync(cmd);
console.log('Saved:', outDevicePng);

// Clean temp html
fs.unlinkSync(tempHtml);
