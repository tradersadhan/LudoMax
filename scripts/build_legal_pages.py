#!/usr/bin/env python3
"""Builds the offline HTML pages (privacy policy, terms, how to play).

They are written to app/src/main/assets/ (shown inside the app) and docs/ (host them on
GitHub Pages - Google Play needs a public Privacy Policy URL).

Placeholders {{DEVELOPER_NAME}}, {{CONTACT_EMAIL}}, {{COUNTRY}} are replaced by
scripts/set_contact.sh - run it once before you publish.
"""
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
DOCS = os.path.join(ROOT, "docs")
DATE = "3 November 2026"

CSS = """
<style>
:root{color-scheme:dark}
*{box-sizing:border-box}
body{margin:0;padding:20px 18px 48px;background:transparent;color:#EAF1FF;
font:16px/1.6 -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif}
.wrap{max-width:760px;margin:0 auto}
h1{font-size:26px;color:#FFE94A;margin:6px 0 4px}
h2{font-size:19px;color:#7FD6FF;margin:26px 0 6px}
p,li{color:#DCE7FF}
a{color:#FFE94A}
.meta{color:#9DB4EE;font-size:13px;margin-bottom:18px}
.card{background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.18);border-radius:14px;padding:12px 16px;margin:14px 0}
ul{padding-left:22px}
.dot{display:inline-block;width:12px;height:12px;border-radius:50%;margin-right:6px;vertical-align:middle}
</style>
"""

def page(title, body, standalone):
    bg = "background:linear-gradient(160deg,#031A8A,#0A2FC2 45%,#020B4F);min-height:100vh;" if standalone else ""
    return f"""<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title} - LudoMax</title>
{CSS}
<style>html{{{bg}}}</style></head>
<body><div class="wrap">
{body}
</div></body></html>
"""

PRIVACY = f"""
<h1>Privacy Policy</h1>
<div class="meta">LudoMax &bull; Effective date: {DATE}</div>

<p>This Privacy Policy explains how <b>LudoMax</b> (the &ldquo;App&rdquo;), published by
<b>{{{{DEVELOPER_NAME}}}}</b> (&ldquo;we&rdquo;, &ldquo;us&rdquo;), handles information. We built LudoMax as a
simple, fully offline game. <b>We do not collect, store, share or sell any personal information.</b></p>

<div class="card"><b>In short:</b> no account, no sign-up, no ads, no analytics, no tracking, no internet
access. Everything stays on your device.</div>

<h2>1. Information we collect</h2>
<p>None. LudoMax does not ask for, collect or transmit your name, e-mail address, phone number, contacts,
photos, location, microphone, camera, advertising ID or any other personal data.</p>

<h2>2. Data stored on your device</h2>
<p>The App stores only your game preferences (for example whether music and sound effects are switched on)
in the App&rsquo;s private storage on your device. This data never leaves your device and is removed when you
clear the App&rsquo;s data or uninstall it.</p>

<h2>3. Internet and permissions</h2>
<p>LudoMax works completely offline and does <b>not</b> request the INTERNET permission or any other
sensitive permission. The Privacy Policy, Terms and How-to-Play screens are bundled inside the App.</p>

<h2>4. Third-party services</h2>
<p>The App does not include advertising networks, analytics, crash-reporting or social-media SDKs, and does not
contain in-app purchases. The &ldquo;Share&rdquo; button simply opens your phone&rsquo;s standard share sheet; what
happens next is governed by the app you choose.</p>

<h2>5. Children&rsquo;s privacy</h2>
<p>Because we collect no data at all, the App can be used by people of all ages. We do not knowingly collect
information from anyone, including children under 13 (or the minimum age in your country).</p>

<h2>6. Google Play</h2>
<p>If you installed the App from Google Play, Google may collect certain information as described in
<a href="https://policies.google.com/privacy">Google&rsquo;s Privacy Policy</a>. That is outside our control.</p>

<h2>7. Security</h2>
<p>Since the App holds no personal data and has no network access, there is nothing for us to transmit or
expose. Your preferences are stored using Android&rsquo;s standard private app storage.</p>

<h2>8. Changes to this policy</h2>
<p>If we change this policy we will update the effective date above and publish the new version in the App
and on our policy page. Continued use after a change means you accept the updated policy.</p>

<h2>9. Contact us</h2>
<p>Questions? Write to <a href="mailto:{{{{CONTACT_EMAIL}}}}">{{{{CONTACT_EMAIL}}}}</a>.</p>
"""

TERMS = f"""
<h1>Terms &amp; Conditions</h1>
<div class="meta">LudoMax &bull; Effective date: {DATE}</div>

<p>Please read these Terms &amp; Conditions (&ldquo;Terms&rdquo;) before using <b>LudoMax</b> (the
&ldquo;App&rdquo;), published by <b>{{{{DEVELOPER_NAME}}}}</b>. By installing or using the App you agree to these
Terms. If you do not agree, please do not use the App.</p>

<h2>1. Licence</h2>
<p>We grant you a personal, non-exclusive, non-transferable, revocable licence to install and use the App on
your own devices for personal, non-commercial entertainment.</p>

<h2>2. Entertainment only &ndash; no real money</h2>
<p>LudoMax is a free board-game for fun. It contains <b>no gambling, no real-money play, no wagering and no
prizes</b>. Nothing in the App has monetary value.</p>

<h2>3. Acceptable use</h2>
<ul>
<li>Do not copy, modify, decompile, reverse-engineer or redistribute the App or its artwork, music and sounds,
except where the law allows it despite this restriction.</li>
<li>Do not use the App for any unlawful purpose or in a way that could damage it or interfere with
other users&rsquo; devices.</li>
<li>Do not remove or alter any copyright, trademark or other notices.</li>
</ul>

<h2>4. Intellectual property</h2>
<p>The App, including its name, logo, graphics, animations, music, sound effects, source code and
game design, is owned by or licensed to {{{{DEVELOPER_NAME}}}} and protected by copyright and other laws.
&ldquo;Ludo&rdquo; is a traditional board game; LudoMax is an independent, original implementation and is not
affiliated with or endorsed by any other game or company.</p>

<h2>5. Fair play and game rules</h2>
<p>Dice rolls are generated randomly on your device. Computer opponents follow the rules described in
&ldquo;How to Play&rdquo;. Game results are for amusement only.</p>

<h2>6. Privacy</h2>
<p>Your use of the App is also governed by our Privacy Policy. In short: the App is offline and collects no
personal data.</p>

<h2>7. Updates and availability</h2>
<p>We may update, change or discontinue features of the App at any time. We are not obliged to provide
updates or support, but we will try to fix reported problems where reasonable.</p>

<h2>8. Disclaimer of warranties</h2>
<p>The App is provided <b>&ldquo;as is&rdquo; and &ldquo;as available&rdquo;</b> without warranties of any kind, express
or implied, including fitness for a particular purpose, uninterrupted or error-free operation, or
compatibility with every device.</p>

<h2>9. Limitation of liability</h2>
<p>To the maximum extent permitted by law, {{{{DEVELOPER_NAME}}}} shall not be liable for any indirect,
incidental, special or consequential damages, or for loss of data, arising from your use of or inability to use
the App. Nothing in these Terms limits liability that cannot be limited by law.</p>

<h2>10. Termination</h2>
<p>You may stop using the App at any time by uninstalling it. We may terminate your licence if you breach
these Terms.</p>

<h2>11. Changes to these Terms</h2>
<p>We may revise these Terms from time to time. The updated version will show a new effective date. Continued
use of the App after a change means you accept the new Terms.</p>

<h2>12. Governing law</h2>
<p>These Terms are governed by the laws of {{{{COUNTRY}}}}, without regard to conflict-of-law rules. Mandatory
consumer-protection rights in your country of residence remain unaffected.</p>

<h2>13. Contact</h2>
<p>Questions about these Terms? Write to
<a href="mailto:{{{{CONTACT_EMAIL}}}}">{{{{CONTACT_EMAIL}}}}</a>.</p>
"""

HOWTO = """
<h1>How to Play</h1>
<div class="meta">LudoMax &bull; Quick rules</div>

<div class="card"><b>Goal:</b> bring both of your pawns around the board and into the centre before
your opponents do.</div>

<h2>Players &amp; colours</h2>
<p>Play with 2, 3 or 4 players &mdash; against the computer or with friends on one phone. Each player has
<b>2 pawns</b>.
<span class="dot" style="background:#22B14C"></span>Green
<span class="dot" style="background:#ED1C24"></span>Red
<span class="dot" style="background:#00A2E8"></span>Blue
<span class="dot" style="background:#FFC90E"></span>Yellow.</p>

<h2>Your turn</h2>
<ul>
<li><b>Tap your dice</b> to roll it.</li>
<li>You need a <b>6</b> to bring a pawn out of its home yard onto your coloured start square.</li>
<li>Pawns move <b>clockwise</b> by the number on the dice. If you have two possible moves, tap the pawn you
want to move (the glowing pawns can move). If only one move is possible, it is played for you.</li>
<li>If no pawn can move, the turn passes to the next player.</li>
</ul>

<h2>Bonus turns</h2>
<ul>
<li>Rolling a <b>6</b> gives you another roll.</li>
<li><b>Capturing</b> an opponent&rsquo;s pawn gives you another roll.</li>
<li>Bringing a pawn <b>home</b> gives you another roll.</li>
<li>Three 6s in a row cancel your turn.</li>
</ul>

<h2>Capturing</h2>
<p>If you land exactly on a square occupied by a single opponent pawn, that pawn is sent back to its yard and
must wait for a 6 again.</p>

<h2>Safe squares</h2>
<ul>
<li>The four <b>coloured start squares</b> (with a white star) are always safe.</li>
<li>The four <b>grey star squares</b> are safe too.</li>
<li>Two pawns of the same colour on one square protect each other.</li>
<li>Your coloured <b>home column</b> is safe &mdash; opponents cannot enter it.</li>
</ul>

<h2>Going home</h2>
<p>After a full lap your pawn turns into your coloured home column at the arrow. You must roll the
<b>exact number</b> needed to reach the centre.</p>

<h2>Winning</h2>
<p>The first player to bring both pawns to the centre wins. In 3&ndash;4 player games the others keep playing for 2nd
and 3rd place.</p>

<div class="card"><b>Tip:</b> Use the music and sound buttons at the top of the game screen, or the
pause button for restart options.</div>
"""

PAGES = {
    "privacy_policy.html": ("Privacy Policy", PRIVACY),
    "terms_and_conditions.html": ("Terms & Conditions", TERMS),
    "how_to_play.html": ("How to Play", HOWTO),
}

os.makedirs(ASSETS, exist_ok=True)
os.makedirs(DOCS, exist_ok=True)
for fname, (title, body) in PAGES.items():
    with open(os.path.join(ASSETS, fname), "w", encoding="utf-8") as f:
        f.write(page(title, body, standalone=False))
    with open(os.path.join(DOCS, fname), "w", encoding="utf-8") as f:
        f.write(page(title, body, standalone=True))
    print("wrote", fname)

index = """<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>LudoMax</title>""" + CSS + """<style>html{background:linear-gradient(160deg,#031A8A,#0A2FC2 45%,#020B4F);min-height:100vh}</style></head>
<body><div class="wrap">
<h1>LudoMax</h1>
<p>Classic Ludo, 100% offline. Play against the computer or with friends.</p>
<ul>
<li><a href="privacy_policy.html">Privacy Policy</a></li>
<li><a href="terms_and_conditions.html">Terms &amp; Conditions</a></li>
<li><a href="how_to_play.html">How to Play</a></li>
</ul></div></body></html>
"""
with open(os.path.join(DOCS, "index.html"), "w", encoding="utf-8") as f:
    f.write(index)
print("wrote docs/index.html")
