import re

def convert_html_to_jsx():
    with open("d:\\Micro Project\\index.html", "r", encoding="utf-8") as f:
        html = f.read()
        
    # Extract style
    style_match = re.search(r'<style>(.*?)</style>', html, re.DOTALL)
    if style_match:
        with open("d:\\Micro Project\\frontend\\src\\pages\\LandingPage.css", "w", encoding="utf-8") as f:
            f.write(style_match.group(1).strip())
            
    # Extract body content (between <body> and </body>, excluding scripts)
    body_match = re.search(r'<body[^>]*>(.*?)<script crossorigin=""', html, re.DOTALL)
    if not body_match:
        body_match = re.search(r'<body[^>]*>(.*?)</body>', html, re.DOTALL)
        
    body_html = body_match.group(1).strip()
    
    # Remove the <script> block for the React scan-picker because we will write it natively
    body_html = re.sub(r'<script.*?</script>', '', body_html, flags=re.DOTALL)
    
    # Replacements for JSX
    body_html = body_html.replace('class=', 'className=')
    body_html = body_html.replace('for=', 'htmlFor=')
    body_html = body_html.replace('<!--', '{/*')
    body_html = body_html.replace('-->', '*/}')
    body_html = body_html.replace('style="text-wrap: balance;"', 'style={{ textWrap: "balance" }}')
    body_html = body_html.replace("style=\"font-variation-settings: 'FILL' 1;\"", "style={{ fontVariationSettings: \"'FILL' 1\" }}")
    body_html = body_html.replace('style="transition-delay:0.1s"', 'style={{ transitionDelay: "0.1s" }}')
    body_html = body_html.replace('style="transition-delay:0.2s"', 'style={{ transitionDelay: "0.2s" }}')
    body_html = body_html.replace('stroke-width', 'strokeWidth')
    body_html = body_html.replace('stroke-linecap', 'strokeLinecap')
    body_html = body_html.replace('stroke-dasharray', 'strokeDasharray')
    body_html = body_html.replace('stroke-dashoffset', 'strokeDashoffset')
    body_html = body_html.replace('href="register.html"', 'href="/register"')
    body_html = body_html.replace('Free forever\n            </span>', 'Free forever')
    
    # Self close tags
    body_html = re.sub(r'<br/?>', '<br />', body_html)
    body_html = re.sub(r'<img([^>]+?)(?<!/)>', r'<img\1 />', body_html)
    body_html = re.sub(r'<input([^>]+?)(?<!/)>', r'<input\1 />', body_html)
    body_html = re.sub(r'<hr([^>]+?)(?<!/)>', r'<hr\1 />', body_html)
    
    # Replace <a> with <Link> only for specific routes
    body_html = re.sub(r'<a href="login.html"([^>]*)>(.*?)</a>', r'<Link to="/login"\1>\2</Link>', body_html, flags=re.DOTALL)
    body_html = re.sub(r'<a href="register.html"([^>]*)>(.*?)</a>', r'<Link to="/register"\1>\2</Link>', body_html, flags=re.DOTALL)
    
    # Generate the JSX wrapper
    jsx_content = f"""import React, {{ useEffect, useState }} from 'react';
import {{ Link }} from 'react-router-dom';
import './LandingPage.css';

const TABS = [
  {{
    key: "file",
    label: "File",
    icon: "upload_file",
    heading: "sample_invoice.pdf",
    lines: [
      "Unpacking archive… 3 embedded objects found",
      "Scanning embedded macros for obfuscation",
      "Cross-referencing against known payload signatures",
    ],
    verdict: "No malicious payloads detected",
    verdictType: "safe",
  }},
  {{
    key: "code",
    label: "Source code",
    icon: "code",
    heading: "api/routes/payments.ts",
    lines: [
      "Tracing request data through 4 call sites",
      "Checking secrets and hard-coded credentials",
      "Modeling auth bypass and privilege paths",
    ],
    verdict: "Hard-coded API key on line 112",
    verdictType: "danger",
  }},
];

function ScanPicker() {{
  const [active, setActive] = useState("file");
  const tab = TABS.find(t => t.key === active);

  return (
    <div className="glass-panel border border-outline-variant/50 rounded-xl p-lg md:p-xl">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-md mb-lg">
        <div>
          <h3 className="font-title-lg text-title-lg text-on-surface font-bold">Try a preview scan</h3>
          <p className="font-body-md text-body-md text-on-surface-variant mt-xs">See how each scanner reasons before you connect anything real.</p>
        </div>
        <div className="flex gap-xs bg-surface-container-high rounded-lg p-xs w-fit">
          {{TABS.map(t => (
            <button
              key={{t.key}}
              onClick={{() => setActive(t.key)}}
              className={{`flex items-center gap-xs px-md py-sm rounded font-label-md text-label-md font-semibold transition-all ${{
                active === t.key ? "bg-surface-container-lowest text-primary shadow-sm" : "text-on-surface-variant hover:text-on-surface"
              }}`}}
            >
              <span className="material-symbols-outlined text-[16px]">{{t.icon}}</span>
              {{t.label}}
            </button>
          ))}}
        </div>
      </div>

      <div className="bg-inverse-surface rounded-lg p-lg font-mono-code text-[13px]">
        <p className="text-[#8b96ab] mb-md">$ aegis scan --target {{tab.heading}}</p>
        {{tab.lines.map((line, i) => (
          <p key={{i}} className="text-[#d3e4fe] mb-xs">› {{line}}</p>
        ))}}
        <p className={{`mt-md flex items-center gap-xs font-semibold ${{tab.verdictType === "safe" ? "text-[#a9f2d4]" : "text-[#ffb4ab]"}}`}}>
          <span className="material-symbols-outlined text-[16px]">{{tab.verdictType === "safe" ? "verified" : "error"}}</span>
          {{tab.verdict}}
        </p>
      </div>
    </div>
  );
}}

export default function LandingPage() {{
  useEffect(() => {{
    // Sticky header background on scroll
    const header = document.getElementById('site-header');
    const onScroll = () => {{
        if (window.scrollY > 8) {{
            header.classList.add('bg-surface/80', 'backdrop-blur-md', 'border-outline-variant/50');
        }} else {{
            header.classList.remove('bg-surface/80', 'backdrop-blur-md', 'border-outline-variant/50');
        }}
    }};
    window.addEventListener('scroll', onScroll);

    // Scroll reveal
    const revealEls = document.querySelectorAll('.reveal');
    const revealObserver = new IntersectionObserver((entries) => {{
        entries.forEach(entry => {{
            if (entry.isIntersecting) {{
                entry.target.classList.add('in-view');
                revealObserver.unobserve(entry.target);
            }}
        }});
    }}, {{ threshold: 0.15 }});
    revealEls.forEach(el => revealObserver.observe(el));

    // Animated counters
    function animateCount(el) {{
        const target = parseFloat(el.dataset.target);
        const decimals = parseInt(el.dataset.decimals || "0");
        const suffix = el.dataset.suffix || "";
        const duration = 1400;
        const start = performance.now();
        function tick(now) {{
            const progress = Math.min((now - start) / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            const value = target * eased;
            el.textContent = (decimals ? value.toFixed(decimals) : Math.floor(value).toLocaleString()) + suffix;
            if (progress < 1) requestAnimationFrame(tick);
        }}
        requestAnimationFrame(tick);
    }}
    const countObserver = new IntersectionObserver((entries) => {{
        entries.forEach(entry => {{
            if (entry.isIntersecting) {{
                entry.target.setAttribute('data-animated', 'true');
                animateCount(entry.target);
                countObserver.unobserve(entry.target);
            }}
        }});
    }}, {{ threshold: 0.5 }});
    document.querySelectorAll('.stat-count').forEach(el => countObserver.observe(el));

    // Hero terminal: reveal finding badge partway through the sweep, then allow "Fix with AI"
    const statusBadge = document.getElementById('status-badge');
    const statusIcon = document.getElementById('status-icon');
    const statusText = document.getElementById('status-text');
    const gaugeArc = document.getElementById('gauge-arc');
    const gaugeNumber = document.getElementById('gauge-number');
    const vulnLine = document.getElementById('vuln-line');
    const vulnCode = document.getElementById('vuln-code');
    const aiFixBtn = document.getElementById('ai-fix-btn');
    const CIRC = 150.8;
    let fixed = false;

    function setGauge(score, color) {{
        if(gaugeArc) {{
            const offset = CIRC - (CIRC * score / 100);
            gaugeArc.style.strokeDashoffset = offset;
            gaugeArc.setAttribute('stroke', color);
            if(gaugeNumber) gaugeNumber.textContent = score;
        }}
    }}

    function showFinding() {{
        if(vulnLine) vulnLine.classList.add('flag-danger');
        if(statusBadge) requestAnimationFrame(() => statusBadge.style.opacity = 1);
    }}
    const timeoutId = setTimeout(showFinding, 1900);

    const onAiFix = () => {{
        if (fixed) {{
            fixed = false;
            if(vulnCode) vulnCode.textContent = '    query = "SELECT * FROM users WHERE id=" + user_id';
            if(vulnLine) {{
                vulnLine.classList.remove('flag-safe');
                vulnLine.classList.add('flag-danger');
            }}
            if(statusBadge) statusBadge.className = "flex items-center gap-sm bg-error-container/80 text-on-error-container px-md py-sm rounded-lg font-label-md text-label-md font-semibold transition-all duration-300 shadow-[0_0_20px_rgba(255,107,107,0.25)]";
            if(statusIcon) statusIcon.textContent = "error";
            if(statusText) statusText.textContent = "SQL Injection · CWE-89 · Critical";
            
            setGauge(42, '#ff6b6b');
            if(aiFixBtn) aiFixBtn.innerHTML = '<span className="material-symbols-outlined text-[16px]">auto_fix_high</span>Fix with AI';
        }} else {{
            fixed = true;
            if(vulnCode) vulnCode.textContent = '    query = "SELECT * FROM users WHERE id=?", (user_id,)';
            if(vulnLine) {{
                vulnLine.classList.remove('flag-danger');
                vulnLine.classList.add('flag-safe');
            }}
            
            if(statusBadge) statusBadge.className = "flex items-center gap-sm bg-tertiary-container/20 text-tertiary-container px-md py-sm rounded-lg font-label-md text-label-md font-semibold transition-all duration-300";
            if(statusIcon) statusIcon.textContent = "verified";
            if(statusText) statusText.textContent = "Secured by Aegis Nexus AI";
            
            setGauge(98, '#4edea3');
            if(aiFixBtn) aiFixBtn.innerHTML = '<span className="material-symbols-outlined text-[16px]">replay</span>Revert fix';
        }}
    }};
    
    if(aiFixBtn) aiFixBtn.addEventListener('click', onAiFix);

    return () => {{
        window.removeEventListener('scroll', onScroll);
        revealObserver.disconnect();
        countObserver.disconnect();
        clearTimeout(timeoutId);
        if(aiFixBtn) aiFixBtn.removeEventListener('click', onAiFix);
    }};
  }}, []);

  return (
    <div className="font-['Inter'] bg-[#f9f9ff] text-[#111c2d]">
      {body_html}
    </div>
  );
}}
"""
    
    # We also need to fix <div id="scan-picker-root"></div>
    jsx_content = jsx_content.replace('<div id="scan-picker-root"></div>', '<ScanPicker />')
    
    with open("d:\\Micro Project\\frontend\\src\\pages\\LandingPage.jsx", "w", encoding="utf-8") as f:
        f.write(jsx_content)

if __name__ == "__main__":
    convert_html_to_jsx()
