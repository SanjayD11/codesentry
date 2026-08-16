import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import './LandingPage.css';

const TABS = [
  {
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
  },
  {
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
  },
];

function ScanPicker() {
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
          {TABS.map(t => (
            <button
              key={t.key}
              onClick={() => setActive(t.key)}
              className={`flex items-center gap-xs px-md py-sm rounded font-label-md text-label-md font-semibold transition-all ${
                active === t.key ? "bg-surface-container-lowest text-primary shadow-sm" : "text-on-surface-variant hover:text-on-surface"
              }`}
            >
              <span className="material-symbols-outlined text-[16px]">{t.icon}</span>
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-inverse-surface rounded-lg p-lg font-mono-code text-[13px]">
        <p className="text-[#8b96ab] mb-md">$ aegis scan --target {tab.heading}</p>
        {tab.lines.map((line, i) => (
          <p key={i} className="text-[#d3e4fe] mb-xs">› {line}</p>
        ))}
        <p className={`mt-md flex items-center gap-xs font-semibold ${tab.verdictType === "safe" ? "text-[#a9f2d4]" : "text-[#ffb4ab]"}`}>
          <span className="material-symbols-outlined text-[16px]">{tab.verdictType === "safe" ? "verified" : "error"}</span>
          {tab.verdict}
        </p>
      </div>
    </div>
  );
}

export default function LandingPage() {
  useEffect(() => {
    // Sticky header background on scroll
    const header = document.getElementById('site-header');
    const onScroll = () => {
        if (window.scrollY > 8) {
            header.classList.add('bg-surface/80', 'backdrop-blur-md', 'border-outline-variant/50');
        } else {
            header.classList.remove('bg-surface/80', 'backdrop-blur-md', 'border-outline-variant/50');
        }
    };
    window.addEventListener('scroll', onScroll);

    // Scroll reveal
    const revealEls = document.querySelectorAll('.reveal');
    const revealObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('in-view');
                revealObserver.unobserve(entry.target);
            }
        });
    }, { threshold: 0.15 });
    revealEls.forEach(el => revealObserver.observe(el));

    // Animated counters
    function animateCount(el) {
        const target = parseFloat(el.dataset.target);
        const decimals = parseInt(el.dataset.decimals || "0");
        const suffix = el.dataset.suffix || "";
        const duration = 1400;
        const start = performance.now();
        function tick(now) {
            const progress = Math.min((now - start) / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            const value = target * eased;
            el.textContent = (decimals ? value.toFixed(decimals) : Math.floor(value).toLocaleString()) + suffix;
            if (progress < 1) requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);
    }
    const countObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.setAttribute('data-animated', 'true');
                animateCount(entry.target);
                countObserver.unobserve(entry.target);
            }
        });
    }, { threshold: 0.5 });
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

    function setGauge(score, color) {
        if(gaugeArc) {
            const offset = CIRC - (CIRC * score / 100);
            gaugeArc.style.strokeDashoffset = offset;
            gaugeArc.setAttribute('stroke', color);
            if(gaugeNumber) gaugeNumber.textContent = score;
        }
    }

    function showFinding() {
        if(vulnLine) vulnLine.classList.add('flag-danger');
        if(statusBadge) requestAnimationFrame(() => statusBadge.style.opacity = 1);
    }
    const timeoutId = setTimeout(showFinding, 1900);

    const onAiFix = () => {
        if (fixed) {
            fixed = false;
            if(vulnCode) vulnCode.textContent = '    query = "SELECT * FROM users WHERE id=" + user_id';
            if(vulnLine) {
                vulnLine.classList.remove('flag-safe');
                vulnLine.classList.add('flag-danger');
            }
            if(statusBadge) statusBadge.className = "flex items-center gap-sm bg-error-container/80 text-on-error-container px-md py-sm rounded-lg font-label-md text-label-md font-semibold transition-all duration-300 shadow-[0_0_20px_rgba(255,107,107,0.25)]";
            if(statusIcon) statusIcon.textContent = "error";
            if(statusText) statusText.textContent = "SQL Injection · CWE-89 · Critical";
            
            setGauge(42, '#ff6b6b');
            if(aiFixBtn) aiFixBtn.innerHTML = '<span className="material-symbols-outlined text-[16px]">auto_fix_high</span>Fix with AI';
        } else {
            fixed = true;
            if(vulnCode) vulnCode.textContent = '    query = "SELECT * FROM users WHERE id=?", (user_id,)';
            if(vulnLine) {
                vulnLine.classList.remove('flag-danger');
                vulnLine.classList.add('flag-safe');
            }
            
            if(statusBadge) statusBadge.className = "flex items-center gap-sm bg-tertiary-container/20 text-tertiary-container px-md py-sm rounded-lg font-label-md text-label-md font-semibold transition-all duration-300";
            if(statusIcon) statusIcon.textContent = "verified";
            if(statusText) statusText.textContent = "Secured by Aegis Nexus AI";
            
            setGauge(98, '#4edea3');
            if(aiFixBtn) aiFixBtn.innerHTML = '<span className="material-symbols-outlined text-[16px]">replay</span>Revert fix';
        }
    };
    
    if(aiFixBtn) aiFixBtn.addEventListener('click', onAiFix);

    return () => {
        window.removeEventListener('scroll', onScroll);
        revealObserver.disconnect();
        countObserver.disconnect();
        clearTimeout(timeoutId);
        if(aiFixBtn) aiFixBtn.removeEventListener('click', onAiFix);
    };
  }, []);

  return (
    <div className="font-['Inter'] bg-[#f9f9ff] text-[#111c2d]">
      {/* ============ NAV ============ */}
<header id="site-header" className="fixed top-0 inset-x-0 z-50 border-b border-transparent transition-all duration-300">
    <div className="max-w-max-width mx-auto px-margin-mobile md:px-margin-desktop h-16 flex items-center justify-between">
        <a href="#top" className="flex items-center gap-sm">
            <div className="w-9 h-9 rounded-lg bg-primary-container flex items-center justify-center text-on-primary-container shadow-sm">
                <span className="material-symbols-outlined text-[20px]" style={{ fontVariationSettings: "'FILL' 1" }}>security</span>
            </div>
            <span className="font-headline-md text-[18px] font-extrabold tracking-tight text-primary">Aegis Nexus</span>
        </a>
        <nav className="hidden md:flex items-center gap-2xl font-label-md text-label-md text-on-surface-variant">
            <a href="#capabilities" className="hover:text-primary transition-colors font-medium">Capabilities</a>
            <a href="#how-it-works" className="hover:text-primary transition-colors font-medium">How it works</a>
            <a href="#assistant" className="hover:text-primary transition-colors font-medium">Analysis Assistant</a>
        </nav>
        <div className="hidden md:flex items-center gap-md">
            Free forever
            <Link to="/login" className="cta-magnetic bg-primary text-on-primary px-lg py-sm rounded-lg font-label-md text-label-md font-semibold shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_1px_2px_rgba(0,0,0,0.1)] hover:bg-primary-fixed-variant transition-all hover:shadow-md">
                Start scanning
            </Link>
        </div>
        <button id="mobile-menu-btn" className="md:hidden p-sm text-on-surface">
            <span className="material-symbols-outlined" id="mobile-menu-icon">menu</span>
        </button>
    </div>
    <div id="mobile-menu" className="hidden md:hidden bg-surface-container-lowest border-t border-outline-variant/50 px-margin-mobile py-md flex flex-col gap-sm">
        <a href="#capabilities" className="py-sm text-on-surface-variant font-medium">Capabilities</a>
        <a href="#how-it-works" className="py-sm text-on-surface-variant font-medium">How it works</a>
        <a href="#assistant" className="py-sm text-on-surface-variant font-medium">AI Assistant</a>
        <Link to="/login" className="mt-sm bg-primary text-on-primary text-center px-lg py-sm rounded-lg font-label-md font-semibold">Start scanning</Link>
    </div>
</header>

<main id="top">

{/* ============ HERO ============ */}
<section className="mesh-bg pt-32 pb-3xl md:pt-40 md:pb-3xl overflow-hidden relative">
    <div className="max-w-max-width mx-auto px-margin-mobile md:px-margin-desktop grid lg:grid-cols-2 gap-3xl items-center relative z-10">
        <div className="reveal relative min-w-0">
            <div className="hero-grid absolute -inset-x-16 -top-24 h-[420px] -z-10"></div>
            <p className="font-bold text-primary tracking-[0.2em] uppercase text-[12px] mb-6">AI Security Analysis Platform</p>
            <h1 className="text-[40px] leading-[1.15] md:text-[56px] md:leading-[1.1] font-bold tracking-[-0.03em] text-on-surface mb-6" style={{ textWrap: "balance" }}>
                <span className="text-primary">Secure Your Code.</span><br className="hidden md:block"/> Understand Every Vulnerability.
            </h1>
            <p className="font-body-lg text-body-lg text-on-surface-variant max-w-[440px]">
                An AI-assisted platform that evaluates your source code and files. Identify hidden vulnerabilities, get plain-language explanations, and apply actionable fixes.
            </p>
            <div id="hero-cta" className="flex flex-col sm:flex-row gap-md mt-xl">
                <Link to="/login" className="cta-magnetic bg-primary text-on-primary px-xl py-md rounded-lg font-label-md text-label-md font-semibold shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_1px_2px_rgba(0,0,0,0.1)] hover:bg-primary-fixed-variant transition-all hover:shadow-md flex items-center justify-center gap-sm">
                    Start Analysis
                </Link>
                <a href="#capabilities" className="cta-magnetic bg-surface-container-lowest border border-outline-variant text-on-surface px-xl py-md rounded-lg font-label-md text-label-md font-semibold hover:bg-surface-container-low transition-all flex items-center justify-center gap-sm">
                    Explore Platform
                </a>
            </div>
            
        </div>

        {/* Signature element: live scan terminal */}
        <div className="reveal min-w-0 w-full">
            <div className="terminal-shell max-w-[520px] mx-auto w-full">
                <div className="terminal-bar px-lg py-sm flex items-center justify-between">
                    <div className="flex items-center gap-sm">
                        <span className="w-3 h-3 rounded-full bg-[#ff5f56]"></span>
                        <span className="w-3 h-3 rounded-full bg-[#ffbd2e]"></span>
                        <span className="w-3 h-3 rounded-full bg-[#27c93f]"></span>
                    </div>
                    <span className="font-mono-code text-[12px] text-[#8b96ab] truncate max-w-[150px] sm:max-w-none">auth_handler.py — Aegis Nexus scan</span>
                    <span className="material-symbols-outlined text-[16px] text-[#8b96ab]">radar</span>
                </div>
                <div className="terminal-body pt-lg pb-md">
                    <div className="code-block-wrap mx-lg">
                        <div className="scan-beam" id="scan-beam"></div>
                        <div className="font-mono-code text-[13px] leading-[22px]" id="code-lines">
                            <div className="code-line !px-0"><span className="ln">1</span><span className="code">def get_user(request):</span></div>
                            <div className="code-line !px-0"><span className="ln">2</span><span className="code">    user_id = request.args.get("id")</span></div>
                            <div className="code-line !px-0"><span className="ln">3</span><span className="code">    conn = db.connect()</span></div>
                            <div className="code-line !px-0" id="vuln-line" data-state="danger"><span className="ln">4</span><span className="code" id="vuln-code">    query = "SELECT * FROM users WHERE id=" + user_id</span></div>
                            <div className="code-line !px-0"><span className="ln">5</span><span className="code">    return conn.execute(query).fetchone()</span></div>
                        </div>
                    </div>
                    <div className="px-lg mt-md h-[44px] flex items-center">
                        <div id="status-badge" className="flex items-center gap-sm bg-error-container/80 text-on-error-container px-md py-sm rounded-lg font-label-md text-label-md font-semibold opacity-0 transition-all duration-300 shadow-[0_0_20px_rgba(255,107,107,0.25)]">
                            <span id="status-icon" className="material-symbols-outlined text-[16px]">error</span>
                            <span id="status-text">SQL Injection · CWE-89 · Critical</span>
                        </div>
                    </div>
                    <div className="flex items-center justify-between px-lg mt-lg pt-lg border-t border-white/10">
                        <div className="flex items-center gap-md">
                            <svg width="56" height="56" viewBox="0 0 56 56">
                                <circle cx="28" cy="28" r="24" fill="none" stroke="#334155" strokeWidth="5"/>
                                <circle id="gauge-arc" className="gauge-arc" cx="28" cy="28" r="24" fill="none" stroke="#ff6b6b" strokeWidth="5" strokeLinecap="round" strokeDasharray="150.8" strokeDashoffset="87.46" transform="rotate(-90 28 28)"/>
                            </svg>
                            <div>
                                <p className="font-mono-code text-[11px] text-[#8b96ab] uppercase tracking-wide">Security score</p>
                                <p className="font-mono-code text-[20px] font-bold text-white"><span id="gauge-number">42</span><span className="text-[#8b96ab] text-[13px]">/100</span></p>
                            </div>
                        </div>
                        <button id="ai-fix-btn" className="cta-magnetic bg-tertiary-container text-white px-md py-sm rounded-lg font-label-md text-label-md font-semibold flex items-center gap-xs">
                            <span className="material-symbols-outlined text-[16px]">auto_fix_high</span>
                            Fix with AI
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>

{/* ============ STATS BAND ============ */}
<section className="max-w-max-width mx-auto px-margin-mobile md:px-margin-desktop -mt-2xl relative z-20">
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-lg reveal">
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg hover-lift">
            <div className="p-sm bg-primary-container/10 rounded-lg text-primary ring-1 ring-primary-container/20 w-fit mb-md"><span className="material-symbols-outlined">radar</span></div>
            <h3 className="font-headline-lg text-[26px] md:text-headline-lg text-on-surface font-extrabold tracking-tight"><span className="stat-count" data-target="0" id="stat-scans" >0</span></h3>
            <p className="font-body-md text-body-md text-on-surface-variant font-medium mt-xs">Scans executed</p>
        </div>
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg hover-lift">
            <div className="p-sm bg-error-container/30 rounded-lg text-error ring-1 ring-error-container/40 w-fit mb-md"><span className="material-symbols-outlined">warning</span></div>
            <h3 className="font-headline-lg text-[26px] md:text-headline-lg text-on-surface font-extrabold tracking-tight"><span className="stat-count" data-target="0" id="stat-vulns" >0</span></h3>
            <p className="font-body-md text-body-md text-on-surface-variant font-medium mt-xs">Vulnerabilities identified</p>
        </div>
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg hover-lift">
            <div className="p-sm bg-tertiary-container/10 rounded-lg text-tertiary-container ring-1 ring-tertiary-container/20 w-fit mb-md"><span className="material-symbols-outlined">bolt</span></div>
            <h3 className="font-headline-lg text-[26px] md:text-headline-lg text-on-surface font-extrabold tracking-tight"><span className="stat-count" data-target="0" id="stat-users"  >0</span></h3>
            <p className="font-body-md text-body-md text-on-surface-variant font-medium mt-xs">Registered users</p>
        </div>
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg hover-lift">
            <div className="p-sm bg-secondary-container/30 rounded-lg text-secondary ring-1 ring-secondary-container/40 w-fit mb-md"><span className="material-symbols-outlined">timer</span></div>
            <h3 className="font-headline-lg text-[26px] md:text-headline-lg text-on-surface font-extrabold tracking-tight">&lt;<span className="stat-count" data-target="0" id="stat-projects" >0</span></h3>
            <p className="font-body-md text-body-md text-on-surface-variant font-medium mt-xs">Active projects</p>
        </div>
    </div>
</section>

{/* ============ CAPABILITIES ============ */}
<section id="capabilities" className="max-w-max-width mx-auto px-margin-mobile md:px-margin-desktop pt-3xl pb-2xl">
    <div className="max-w-[600px] mb-2xl reveal">
        <span className="eyebrow-rule font-label-md text-label-md text-primary font-bold uppercase tracking-wide">Capabilities</span>
        <h2 className="font-headline-lg text-[28px] md:text-headline-lg text-on-surface font-extrabold tracking-tight mt-sm">Comprehensive attack surface analysis</h2>
        <p className="font-body-lg text-body-lg text-on-surface-variant mt-md">Analyze your entire application stack. Aegis Nexus applies consistent vulnerability reasoning across file uploads and full code repositories.</p>
    </div>

    <div className="grid md:grid-cols-2 gap-lg reveal">
        <div className="glass-panel border-outline-variant/50 rounded-xl p-xl hover-lift">
            <div className="p-sm bg-primary-container/10 rounded-lg text-primary ring-1 ring-primary-container/20 w-fit mb-lg"><span className="material-symbols-outlined">upload_file</span></div>
            <h3 className="font-title-lg text-title-lg text-on-surface font-bold">File Scanner</h3>
            <p className="font-body-md text-body-md text-on-surface-variant mt-sm">Drop binaries, archives, or documents. The platform unpacks, sandboxes, and inspects structural behavior before deployment.</p>
        </div>
        
        <div className="glass-panel border-outline-variant/50 rounded-xl p-xl hover-lift">
            <div className="p-sm bg-secondary-container/30 rounded-lg text-secondary ring-1 ring-secondary-container/40 w-fit mb-lg"><span className="material-symbols-outlined">code</span></div>
            <h3 className="font-title-lg text-title-lg text-on-surface font-bold">Source Code Scanner</h3>
            <p className="font-body-md text-body-md text-on-surface-variant mt-sm">Connect a repository for deep semantic review. Detect injection paths, authentication logic flaws, and exposed credentials.</p>
        </div>
    </div>

    {/* Slight-React interactive picker */}
    <div className="mt-lg reveal">
        <ScanPicker />
    </div>
</section>

{/* ============ HOW IT WORKS ============ */}
<section id="how-it-works" className="bg-surface-container-low/60 py-3xl">
    <div className="max-w-max-width mx-auto px-margin-mobile md:px-margin-desktop">
        <div className="max-w-[600px] mb-2xl reveal">
            <span className="eyebrow-rule font-label-md text-label-md text-primary font-bold uppercase tracking-wide">Process</span>
            <h2 className="font-headline-lg text-[28px] md:text-headline-lg text-on-surface font-extrabold tracking-tight mt-sm">Streamlined analysis workflow</h2>
        </div>
        <div className="flex flex-col md:flex-row gap-xl md:gap-0 mt-2xl">
            {/* Step 1 */}
            <div className="flex-1 relative md:pr-xl reveal">
                <div className="flex items-center mb-lg">
                    <div className="w-10 h-10 shrink-0 rounded-full bg-primary text-on-primary flex items-center justify-center font-bold text-[14px] z-10 relative shadow-[0_0_15px_rgba(33,112,228,0.3)]">01</div>
                    <div className="hidden md:block h-[2px] bg-outline-variant/30 flex-1 mx-md rounded-full"></div>
                </div>
                <h3 className="font-title-lg text-[18px] text-on-surface font-bold mb-xs">Provide your target</h3>
                <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">Upload a file or provide a source code repository directly. No agents or complex pipeline configurations required.</p>
            </div>
            
            {/* Step 2 */}
            <div className="flex-1 relative md:pr-xl reveal" style={{ transitionDelay: "0.1s" }}>
                <div className="flex items-center mb-lg">
                    <div className="w-10 h-10 shrink-0 rounded-full bg-primary-container/10 text-primary flex items-center justify-center font-bold text-[14px] z-10 relative ring-1 ring-primary-container/20">02</div>
                    <div className="hidden md:block h-[2px] bg-outline-variant/30 flex-1 mx-md rounded-full"></div>
                </div>
                <h3 className="font-title-lg text-[18px] text-on-surface font-bold mb-xs">Context-aware reasoning</h3>
                <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">The analysis engine traces data flows and evaluates application logic, moving beyond static signatures to identify complex vulnerabilities.</p>
            </div>

            {/* Step 3 */}
            <div className="flex-1 relative reveal" style={{ transitionDelay: "0.2s" }}>
                <div className="flex items-center mb-lg">
                    <div className="w-10 h-10 shrink-0 rounded-full bg-primary-container/10 text-primary flex items-center justify-center font-bold text-[14px] z-10 relative ring-1 ring-primary-container/20">03</div>
                </div>
                <h3 className="font-title-lg text-[18px] text-on-surface font-bold mb-xs">Actionable remediation</h3>
                <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">Receive clear, contextual explanations for every finding alongside proposed patches ready for your team's review.</p>
            </div>
        </div>
    </div>
</section>

{/* ============ AI ASSISTANT ============ */}
<section id="assistant" className="max-w-max-width mx-auto px-margin-mobile md:px-margin-desktop py-3xl grid lg:grid-cols-2 gap-3xl items-center">
    <div className="reveal order-2 lg:order-1">
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg max-w-[460px]">
            <div className="flex items-center gap-sm mb-lg pb-md border-b border-outline-variant/50">
                <div className="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center text-on-primary-container"><span className="material-symbols-outlined text-[16px]">smart_toy</span></div>
                <span className="font-label-md text-label-md font-bold text-on-surface">Analysis Assistant</span>
                <span className="ml-auto w-2 h-2 rounded-full bg-tertiary-container"></span>
            </div>
            <div className="space-y-md">
                <div className="bg-surface-container-high rounded-lg rounded-tl-none p-md max-w-[85%]">
                    <p className="font-body-md text-body-md text-on-surface">Why is <code className="font-mono-code text-[12px] bg-surface-container-lowest px-xs py-[1px] rounded">auth_handler.py:4</code> flagged critical?</p>
                </div>
                <div className="bg-primary-container/10 rounded-lg rounded-tr-none p-md ml-auto max-w-[90%]">
                    <p className="font-body-md text-body-md text-on-surface">The query concatenates <code className="font-mono-code text-[12px] bg-surface-container-lowest px-xs py-[1px] rounded">user_id</code> directly into SQL. A request like <code className="font-mono-code text-[12px] bg-surface-container-lowest px-xs py-[1px] rounded">id=1 OR 1=1</code> returns every row. Swap to a parameterized query and the driver escapes input automatically. Want me to open a patch?</p>
                </div>
                <div className="flex gap-sm">
                    <button className="text-label-md font-label-md font-semibold bg-primary text-on-primary px-md py-xs rounded-lg">View patch</button>
                    <button className="text-label-md font-label-md font-semibold border border-outline-variant text-on-surface-variant px-md py-xs rounded-lg">Explain context</button>
                </div>
            </div>
        </div>
    </div>
    <div className="reveal order-1 lg:order-2">
        <span className="eyebrow-rule font-label-md text-label-md text-primary font-bold uppercase tracking-wide">AI Assistant</span>
        <h2 className="font-headline-lg text-[28px] md:text-headline-lg text-on-surface font-extrabold tracking-tight mt-sm">Interactive analysis</h2>
        <p className="font-body-lg text-body-lg text-on-surface-variant mt-md">Understand the exact mechanics of a vulnerability. The assistant analyzes the surrounding context, traces the execution path, and proposes a targeted fix for review. No opaque severity scores without justification.</p>
        <ul className="mt-lg space-y-sm">
            <li className="flex items-start gap-sm"><span className="material-symbols-outlined text-tertiary-container text-[18px] mt-[2px]">check_circle</span><span className="font-body-md text-body-md text-on-surface-variant">Details exploit chains with exact line references</span></li>
            <li className="flex items-start gap-sm"><span className="material-symbols-outlined text-tertiary-container text-[18px] mt-[2px]">check_circle</span><span className="font-body-md text-body-md text-on-surface-variant">Generates specific, reviewable code patches</span></li>
            <li className="flex items-start gap-sm"><span className="material-symbols-outlined text-tertiary-container text-[18px] mt-[2px]">check_circle</span><span className="font-body-md text-body-md text-on-surface-variant">Adapts to your established architectural patterns</span></li>
        </ul>
    </div>
</section>

{/* ============ CORE PLATFORM CAPABILITIES ============ */}
<section className="max-w-max-width mx-auto px-margin-mobile md:px-margin-desktop py-3xl" id="capabilities">
    <div className="max-w-[600px] mb-2xl mx-auto text-center reveal">
        <span className="eyebrow-rule font-label-md text-label-md text-primary font-bold uppercase tracking-wide">Core Features</span>
        <h2 className="font-headline-lg text-[28px] md:text-headline-lg text-on-surface font-extrabold tracking-tight mt-sm">Built for software engineering evaluation</h2>
    </div>
    <div className="grid md:grid-cols-3 gap-lg">
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg reveal flex flex-col hover-lift">
            <span className="material-symbols-outlined text-primary-container text-[28px] mb-md">code</span>
            <h3 className="font-title-lg text-title-lg text-on-surface font-bold mb-sm">Source Code Analysis</h3>
            <p className="font-body-md text-body-md text-on-surface-variant flex-1">Scans Java, Python, JavaScript and other languages for logic flaws, vulnerabilities, and insecure coding patterns.</p>
        </div>
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg reveal flex flex-col hover-lift" style={{ transitionDelay: "0.1s" }}>
            <span className="material-symbols-outlined text-tertiary-container text-[28px] mb-md">upload_file</span>
            <h3 className="font-title-lg text-title-lg text-on-surface font-bold mb-sm">File Security Analysis</h3>
            <p className="font-body-md text-body-md text-on-surface-variant flex-1">Evaluates uploaded files (ZIP, JS, etc.) to detect hidden vulnerabilities before integration into your systems.</p>
        </div>
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg reveal flex flex-col hover-lift" style={{ transitionDelay: "0.2s" }}>
            <span className="material-symbols-outlined text-secondary-container text-[28px] mb-md">analytics</span>
            <h3 className="font-title-lg text-title-lg text-on-surface font-bold mb-sm">Security Report Generation</h3>
            <p className="font-body-md text-body-md text-on-surface-variant flex-1">Automatically compiles detailed vulnerability reports, calculating severity scores based on standard metrics.</p>
        </div>
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg reveal flex flex-col hover-lift">
            <span className="material-symbols-outlined text-primary text-[28px] mb-md">dashboard</span>
            <h3 className="font-title-lg text-title-lg text-on-surface font-bold mb-sm">Interactive Dashboard</h3>
            <p className="font-body-md text-body-md text-on-surface-variant flex-1">Provides a real-time overview of your security posture, recent scan results, and active project metrics.</p>
        </div>
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg reveal flex flex-col hover-lift" style={{ transitionDelay: "0.1s" }}>
            <span className="material-symbols-outlined text-error text-[28px] mb-md">folder_managed</span>
            <h3 className="font-title-lg text-title-lg text-on-surface font-bold mb-sm">Project Management</h3>
            <p className="font-body-md text-body-md text-on-surface-variant flex-1">Organize scans and track remediation progress by grouping related codebases into managed workspaces.</p>
        </div>
        <div className="glass-panel border-outline-variant/50 rounded-xl p-lg reveal flex flex-col hover-lift" style={{ transitionDelay: "0.2s" }}>
            <span className="material-symbols-outlined text-tertiary text-[28px] mb-md">admin_panel_settings</span>
            <h3 className="font-title-lg text-title-lg text-on-surface font-bold mb-sm">Role-Based Access</h3>
            <p className="font-body-md text-body-md text-on-surface-variant flex-1">Secure authentication and authorization integrated with comprehensive audit logs for all administrative actions.</p>
        </div>
    </div>
</section>
{/* ============ ACADEMIC PROJECT ============ */}
<section className="max-w-max-width mx-auto px-margin-mobile md:px-margin-desktop py-2xl">
    <div className="rounded-xl border border-primary-container/30 bg-primary-container/5 p-xl md:p-2xl flex flex-col md:flex-row items-center gap-xl reveal">
        <div className="w-16 h-16 shrink-0 rounded-full bg-primary-container/15 text-primary-container flex items-center justify-center">
            <span className="material-symbols-outlined text-[30px]">school</span>
        </div>
        <div className="flex-1 text-center md:text-left">
            <h3 className="font-title-lg text-title-lg text-on-surface font-bold">Academic Software Engineering Project</h3>
            <p className="font-body-md text-body-md text-on-surface-variant mt-xs">A comprehensive AI-assisted security analysis platform built for academic evaluation, showcasing modern software engineering practices, artificial intelligence integration, and secure development workflows.</p>
        </div>
        <div className="flex flex-col gap-sm shrink-0 font-label-md text-label-md text-on-surface-variant">
            <span className="flex items-center gap-xs"><span className="material-symbols-outlined text-[16px] text-primary-container">check_circle</span>Java Spring Boot Backend</span>
            <span className="flex items-center gap-xs"><span className="material-symbols-outlined text-[16px] text-primary-container">check_circle</span>React & Vite Frontend</span>
        </div>
    </div>
</section>
{/* ============ FINAL CTA ============ */}
<section className="max-w-max-width mx-auto px-margin-mobile md:px-margin-desktop pb-3xl">
    <div className="mesh-bg border border-outline-variant/50 shadow-[0_2px_8px_-2px_rgba(0,0,0,0.05)] rounded-xl p-3xl flex flex-col items-center text-center gap-lg reveal">
        <div className="radar-ring w-16 h-16 rounded-full border-2 border-dashed border-primary/40 flex items-center justify-center">
            <span className="material-symbols-outlined text-primary text-[28px]">radar</span>
        </div>
        <h2 className="font-headline-lg text-[28px] md:text-headline-lg text-on-surface font-extrabold tracking-tight max-w-[520px]">Begin your security analysis</h2>
        <p className="font-body-lg text-body-lg text-on-surface-variant max-w-[480px]">Upload files or connect your source code to evaluate the integrated AI security platform.</p>
        <Link to="/login" className="cta-magnetic bg-primary text-on-primary px-xl py-md rounded-lg font-label-md text-label-md font-semibold shadow-[inset_0_1px_0_rgba(255,255,255,0.2),0_1px_2px_rgba(0,0,0,0.1)] hover:bg-primary-fixed-variant transition-all hover:shadow-md">
            Start scanning
        </Link>
    </div>
</section>

</main>

{/* ============ FOOTER ============ */}
<footer className="border-t border-outline-variant/50 bg-surface-container-lowest py-lg md:py-xl">
    <div className="max-w-max-width mx-auto px-margin-mobile md:px-margin-desktop flex flex-col md:flex-row items-center justify-between gap-lg">
        
        <div className="flex items-center gap-md">
            <div className="w-8 h-8 rounded-lg bg-primary-container flex items-center justify-center text-on-primary-container">
                <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }}>security</span>
            </div>
            <div className="text-left">
                <span className="font-headline-md text-[16px] font-extrabold tracking-tight text-primary leading-tight block">Aegis Nexus</span>
                <span className="font-body-sm text-body-sm text-on-surface-variant leading-tight block mt-[2px]">AI Security Analysis Platform</span>
            </div>
        </div>

        <div className="flex flex-col md:flex-row items-center gap-xs md:gap-md font-body-sm text-body-sm text-on-surface-variant">
            <span>Designed & Developed by Sanjay Dharmarajou</span>
            <span className="hidden md:block w-1 h-1 rounded-full bg-outline-variant/50"></span>
            <span>© 2026 Aegis Nexus. v1.0.0</span>
        </div>
        
    </div>
</footer>

{/* ============ React (slight) — scan-type picker ============ */}
    </div>
  );
}
