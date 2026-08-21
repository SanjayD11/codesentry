import { useState, useEffect, useRef, useCallback } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { sendChat, getConversation, listConversations, deleteConversation } from '../../api/chatApi'
import { getAllUserScans } from '../../api/scanApi'
import { useToast } from '../../hooks/useToast'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import Topnav from '../../components/layout/Topnav'
import BottomNav from '../../components/layout/BottomNav'
import QuickScanModal from './QuickScanModal'
import ConfirmDialog from '../../components/ui/ConfirmDialog'

/* Reusable AI avatar using the bot icon */
function AiAvatar({ size = 36 }) {
  return (
    <img
      src="/ai-bot.png"
      alt="AI Assistant"
      style={{ width: size, height: size, borderRadius: size * 0.28, flexShrink: 0, objectFit: 'cover' }}
    />
  )
}

const SUGGESTIONS = [
  { text: 'Explain my latest security report', icon: 'description' },
  { text: 'What is SQL Injection and how do I fix it?', icon: 'bug_report' },
  { text: 'How can I fix this vulnerability?', icon: 'build' },
  { text: 'Explain my security score', icon: 'speed' },
  { text: 'Secure coding best practices', icon: 'verified_user' },
]

export default function SecurityChat() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const addToast = useToast()
  const reportContextRef = useRef(location.state?.reportContext || null)

  const [conversations, setConversations] = useState([])
  const [activeId, setActiveId] = useState(null)
  const [messages, setMessages] = useState([])
  const [search, setSearch] = useState('')
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [isTyping, setIsTyping] = useState(false)
  const [selectedFile, setSelectedFile] = useState(null)
  const [scans, setScans] = useState([])
  const [selectedScanId, setSelectedScanId] = useState('')
  const [isQuickScanOpen, setIsQuickScanOpen] = useState(false)
  const [scanMenuOpen, setScanMenuOpen] = useState(false)
  const [sidebarOpen, setSidebarOpen] = useState(() => typeof window !== 'undefined' && window.innerWidth >= 1024)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const bottomRef = useRef(null)
  const inputRef = useRef(null)
  const fileInputRef = useRef(null)
  const scanMenuRef = useRef(null)

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (scanMenuRef.current && !scanMenuRef.current.contains(e.target)) {
        setScanMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0])
    }
  }

  const handleRemoveFile = () => {
    setSelectedFile(null)
    if (fileInputRef.current) fileInputRef.current.value = null
  }

  const fetchConversations = useCallback(async () => {
    try {
      const res = await listConversations()
      setConversations(res.data.data || [])
    } catch {
      addToast('Failed to load conversations', 'error')
    }
  }, [addToast])

  const fetchScans = useCallback(async () => {
    try {
      const res = await getAllUserScans()
      setScans(res.data.data || [])
    } catch {
      // ignore
    }
  }, [])

  useEffect(() => { fetchConversations(); fetchScans() }, [fetchConversations, fetchScans])

  useEffect(() => {
    const ctx = reportContextRef.current
    if (!ctx) return
    reportContextRef.current = null // consume once
    const openingMsg = `Please analyze and explain the security report: "${ctx.name || `Report #${ctx.id}`}". Summarize the key findings, explain the severity levels, and highlight the most important recommendations.`
    const t = setTimeout(() => sendMessage(openingMsg), 400)
    return () => clearTimeout(t)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isTyping])

  const loadConversation = async (id) => {
    setLoading(true)
    setActiveId(id)
    if (typeof window !== 'undefined' && window.innerWidth < 1024) {
      setSidebarOpen(false)
    }
    try {
      const res = await getConversation(id)
      setMessages(res.data.data || [])
    } catch {
      addToast('Failed to load messages', 'error')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (activeId === null && conversations.length > 0 && messages.length === 0) {
      // Automatically load the latest conversation on initial visit
      loadConversation(conversations[0].id)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversations])

  const handleNewChat = () => {
    setActiveId(null)
    setMessages([])
    setInput('')
    if (typeof window !== 'undefined' && window.innerWidth < 1024) {
      setSidebarOpen(false)
    }
    inputRef.current?.focus()
  }

  const handleDelete = (e, id) => {
    e.stopPropagation()
    setDeleteTarget(id)
  }

  const handleDeleteConfirm = async () => {
    const id = deleteTarget
    setDeleteTarget(null)
    try {
      await deleteConversation(id)
      addToast('Conversation deleted', 'success')
      if (activeId === id) handleNewChat()
      fetchConversations()
    } catch {
      addToast('Failed to delete', 'error')
    }
  }

  const sendMessage = async (text) => {
    const trimmed = (text || '').trim()
    if ((!trimmed && !selectedFile) || isTyping) return
    setInput('')
    setIsTyping(true)
    const currentFile = selectedFile
    setSelectedFile(null)
    if (fileInputRef.current) fileInputRef.current.value = null

    const tempId = Date.now()
    let displayMessage = trimmed
    let extractedTextToPass = null

    if (currentFile) {
      try {
        extractedTextToPass = await currentFile.text()
      } catch {
        extractedTextToPass = null
      }
    }

    const userMsgObj = {
      id: tempId,
      userMessage: displayMessage,
      attachedFile: currentFile ? currentFile.name : null,
      aiResponse: null
    }

    setMessages(prev => [...prev, userMsgObj])

    try {
      const payload = {
        message: displayMessage || (currentFile ? `[Uploaded file: ${currentFile.name}]` : ''),
        conversationId: activeId,
        scanId: selectedScanId ? Number(selectedScanId) : null
      }

      const res = await sendChat(payload, currentFile)
      const data = res.data.data
      if (data && data.conversationId) setActiveId(data.conversationId)

      setMessages(prev => prev.map(m => m.id === tempId ? { ...m, aiResponse: data.aiResponse } : m))
      fetchConversations()
    } catch (err) {
      addToast(err?.response?.data?.message || 'Failed to send message', 'error')
      setMessages(prev => prev.filter(m => m.id !== tempId))
    } finally {
      setIsTyping(false)
    }
  }

  const filteredConversations = conversations.filter(c => {
    if (!search) return true
    return (c.id && String(c.id).toLowerCase().includes(search.toLowerCase())) ||
           (c.lastMessage && c.lastMessage.toLowerCase().includes(search.toLowerCase()))
  })
  const isFresh = activeId === null && messages.length === 0

  return (
    <div className="sc-chat-outer" style={{ height: '100dvh', display: 'flex', flexDirection: 'column', overflow: 'hidden', background: 'var(--snt-surface-2)', fontFamily: "'Manrope', sans-serif" }}>
      <style>{`
        .chat-sidebar-item { transition: background 0.15s ease; border-radius: 8px; }
        .chat-sidebar-item:hover { background: #f0f3ff; }
        .chat-sidebar-item.active { background: #f0f3ff; border: 1px solid #c2c6d6; }
        .chat-sidebar-item .del-btn { opacity: 0; transition: opacity 0.15s; }
        .chat-sidebar-item:hover .del-btn { opacity: 1; }
        .msg-bubble-ai { animation: msgIn 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
        .msg-bubble-user { animation: msgIn 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
        @keyframes msgIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: none; } }
        .chat-input:focus { border-color: #0058be; box-shadow: 0 0 0 3px rgba(0,88,190,0.1); outline: none; }
        .typing-dot { display: inline-block; width: 6px; height: 6px; background: #0058be; border-radius: 50%; animation: typingBounce 1.2s infinite ease-in-out; }
        .typing-dot:nth-child(2) { animation-delay: 0.2s; }
        .typing-dot:nth-child(3) { animation-delay: 0.4s; }
        @keyframes typingBounce { 0%,80%,100% { transform: scale(0.6); opacity:0.4; } 40% { transform: scale(1); opacity:1; } }
        .suggestion-card { transition: background 0.15s ease, border-color 0.18s ease, box-shadow 0.18s ease; cursor: pointer; }
        .suggestion-card:hover { background: #fff; border-color: #0058be; box-shadow: 0 4px 12px rgba(0,88,190,0.08); }
        .quick-chip { transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease; flex-shrink: 0; }
        .quick-chip:hover { background: #fff; border-color: #0058be; color: #0058be; box-shadow: 0 2px 8px rgba(0,88,190,0.08); }
        ::-webkit-scrollbar { width: 6px; } ::-webkit-scrollbar-track { background: transparent; } ::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 99px; }
        .prose p { margin: 0 0 10px; line-height: 1.65; } .prose p:last-child { margin-bottom: 0; }
        .prose pre { background: #1e293b; color: #e2e8f0; padding: 14px 18px; border-radius: 10px; overflow-x: auto; font-size: 13.5px; margin: 12px 0; }
        .prose code { background: #f0f3ff; color: #0058be; padding: 3px 6px; border-radius: 5px; font-size: 13px; font-weight: 500; }
        .prose pre code { background: transparent; color: inherit; padding: 0; font-weight: 400; }
        .prose ul { padding-left: 22px; margin: 10px 0; } .prose li { margin-bottom: 6px; }
        .send-btn { transition: background 0.15s ease, box-shadow 0.15s ease; }
        .send-btn:hover:not(:disabled) { background: #004395; box-shadow: 0 4px 12px rgba(0,88,190,0.3); }
        .send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
        .sidebar-toggle-btn { transition: background 0.15s ease; border: none; background: none; cursor: pointer; display: flex; align-items: center; justify-content: center; border-radius: 7px; padding: 5px; color: #424754; }
        .sidebar-toggle-btn:hover { background: #f0f3ff; color: #0058be; }
        .sc-chat-outer {
          height: 100dvh;
          box-sizing: border-box;
        }
        /* Quick chips row — horizontal scroll on mobile, wrap on desktop */
        .chips-row { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 2px; scrollbar-width: none; -ms-overflow-style: none; }
        .chips-row::-webkit-scrollbar { display: none; }
        /* Disclaimer hidden on mobile, shown on desktop */
        .chat-disclaimer { display: block; }
        @media (max-width: 1023px) {
          .sc-chat-outer {
            padding-bottom: 64px !important;
            box-sizing: border-box !important;
          }
          .chat-sidebar {
            position: fixed !important;
            top: 56px; bottom: 64px; left: 0;
            z-index: 1100 !important;
            box-shadow: 4px 0 24px rgba(15,23,42,0.18) !important;
          }
          .chat-sidebar[style*="width: 0"] {
            box-shadow: none !important;
            border-right: none !important;
          }
          .chat-input-area {
            padding: 8px 12px 10px !important;
            background: #ffffff !important;
            border-top: 1px solid #e2e6f0 !important;
            box-shadow: 0 -2px 12px rgba(15,23,42,0.06) !important;
          }
          .chat-main-area { padding: 12px 12px 16px !important; }
          .chat-disclaimer { display: none !important; }
        }
        @media (max-width: 767px) {
          .send-btn-text, .hide-mobile { display: none !important; }
          .chat-action-btn { width: 42px !important; height: 42px !important; }
          .chat-input { min-height: 42px !important; height: 42px !important; padding: 9px 12px !important; font-size: 14px !important; }
          .top-action-btn { padding: 5px 10px !important; }
          .chat-top-bar { padding: 0 10px !important; height: 48px !important; }
          .msg-bubble-ai { gap: 10px !important; }
        }
      `}</style>

      {/* ── Top Navigation ─────────────────────────────── */}
      <Topnav />

      {/* ── Body: sidebar + chat ───────────────────────── */}
      <div className="flex flex-1 overflow-hidden relative">

        {/* Mobile Sidebar Overlay Backdrop */}
        {sidebarOpen && typeof window !== 'undefined' && window.innerWidth < 1024 && (
          <div
            onClick={() => setSidebarOpen(false)}
            style={{ position: 'fixed', inset: 0, top: 56, background: 'rgba(15,23,42,0.45)', zIndex: 99 }}
          />
        )}

        {/* ── LEFT SIDEBAR ──────────────────────────────── */}
        <aside
          className="chat-sidebar"
          style={{
            width: sidebarOpen ? 260 : 0,
            minWidth: sidebarOpen ? 260 : 0,
            flexShrink: 0,
            background: '#ffffff',
            borderRight: sidebarOpen ? '1px solid #e2e6f0' : 'none',
            display: 'flex',
            flexDirection: 'column',
            transition: 'width 0.2s ease, min-width 0.2s ease',
            zIndex: 100,
            overflow: 'hidden'
          }}
        >
          {/* Inner wrapper — keeps content from wrapping during animation */}
          <div style={{ width: 260, flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>

            {/* Sidebar Header */}
            <div style={{ padding: '20px 16px 16px', borderBottom: '1px solid #f0f3ff' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <AiAvatar size={32} />
                  <div>
                    <p style={{ margin: 0, fontSize: 14, fontWeight: 700, color: 'var(--snt-text-1)', letterSpacing: '-0.01em', whiteSpace: 'nowrap' }}>AI Assistant</p>
                    <p style={{ margin: 0, fontSize: 11.5, color: 'var(--snt-text-2)', whiteSpace: 'nowrap' }}>Security Analysis</p>
                  </div>
                </div>
                {/* Close / collapse toggle */}
                <button
                  onClick={() => setSidebarOpen(false)}
                  className="sidebar-toggle-btn"
                  title="Collapse sidebar"
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 20 }}>chevron_left</span>
                </button>
              </div>
              <button
                onClick={handleNewChat}
                style={{
                  width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
                  background: '#0058be', color: '#fff', border: 'none', borderRadius: 10,
                  padding: '10px 14px', fontSize: 13.5, fontWeight: 600, cursor: 'pointer',
                  boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.15)', whiteSpace: 'nowrap',
                }}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 16 }}>add</span>
                New conversation
              </button>
            </div>

            {/* Search */}
            <div style={{ padding: '12px 12px 8px' }}>
              <div style={{
                display: 'flex', alignItems: 'center', gap: 8,
                background: '#f9f9ff', border: '1px solid #e2e6f0', borderRadius: 9,
                padding: '8px 12px',
              }}>
                <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#8890a0' }}>search</span>
                <input
                  type="text"
                  placeholder="Search conversations"
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                  style={{ border: 'none', background: 'transparent', outline: 'none', fontSize: 13.5, color: 'var(--snt-text-1)', width: '100%' }}
                />
              </div>
            </div>

            {/* Conversation List */}
            <div style={{ flex: 1, overflowY: 'auto', padding: '4px 8px' }}>
              {filteredConversations.length === 0 ? (
                <div style={{ padding: '32px 12px', textAlign: 'center' }}>
                  <span className="material-symbols-outlined" style={{ fontSize: 32, color: '#c2c6d6' }}>chat_bubble_outline</span>
                  <p style={{ margin: '10px 0 0', fontSize: 13, color: '#8890a0' }}>No conversations yet</p>
                </div>
              ) : filteredConversations.map(item => {
                const threadId = typeof item === 'object' && item !== null ? item.id : item
                const threadLabel = typeof item === 'object' && item?.lastMessage ? item.lastMessage : `Thread · ${String(threadId).substring(0, 8)}`
                return (
                  <div
                    key={threadId}
                    onClick={() => loadConversation(threadId)}
                    className={`chat-sidebar-item ${activeId === threadId ? 'active' : ''}`}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 10,
                      padding: '10px 12px', borderRadius: 8, cursor: 'pointer', marginBottom: 4,
                      border: activeId === threadId ? '1px solid #c2c6d6' : '1px solid transparent',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    <span className="material-symbols-outlined" style={{ fontSize: 16, color: activeId === threadId ? '#0058be' : '#8890a0', flexShrink: 0 }}>chat_bubble</span>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ margin: 0, fontSize: 13, fontWeight: 600, color: 'var(--snt-text-1)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {threadLabel}
                      </p>
                      <p style={{ margin: 0, fontSize: 11, color: '#8890a0' }}>Active session</p>
                    </div>
                    <button
                      onClick={e => handleDelete(e, threadId)}
                      className="del-btn"
                      style={{ border: 'none', background: 'none', padding: 4, cursor: 'pointer', color: '#ba1a1a', borderRadius: 6, display: 'flex', flexShrink: 0 }}
                      title="Delete"
                    >
                      <span className="material-symbols-outlined" style={{ fontSize: 16 }}>delete</span>
                    </button>
                  </div>
                )
              })}
            </div>

            {/* Sidebar Footer */}
            <div style={{ borderTop: '1px solid #f0f3ff', padding: '12px' }}>
              <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', borderRadius: 8, textDecoration: 'none', color: 'var(--snt-text-2)', fontSize: 13.5, fontWeight: 500, whiteSpace: 'nowrap' }}
                onMouseEnter={e => e.currentTarget.style.background = '#f0f3ff'}
                onMouseLeave={e => e.currentTarget.style.background = 'none'}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 16 }}>arrow_back</span>
                Back to Dashboard
              </Link>
            </div>

          </div>{/* end inner wrapper */}
        </aside>

        {/* ── MAIN CHAT AREA ────────────────────────────── */}
        <main style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', background: 'var(--snt-surface-2)' }}>

          {/* Chat Top Bar */}
          <div className="chat-top-bar" style={{
            height: 60, flexShrink: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '0 24px 0 16px',
            background: 'var(--snt-surface)', borderBottom: '1px solid #e2e6f0',
            gap: 12,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0 }}>
              {/* Sidebar open toggle — only visible when sidebar is collapsed */}
              {!sidebarOpen && (
                <button
                  onClick={() => setSidebarOpen(true)}
                  className="sidebar-toggle-btn"
                  title="Open sidebar"
                  style={{ border: 'none', background: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 8, padding: '6px 8px', color: 'var(--snt-text-2)', transition: 'background 0.15s ease', flexShrink: 0 }}
                  onMouseEnter={e => { e.currentTarget.style.background = '#f0f3ff'; e.currentTarget.style.color = '#0058be'; }}
                  onMouseLeave={e => { e.currentTarget.style.background = 'none'; e.currentTarget.style.color = '#424754'; }}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 22 }}>menu</span>
                </button>
              )}
              <div style={{ width: 8, height: 8, borderRadius: '50%', background: isTyping ? '#0058be' : '#00855b', transition: 'background 0.3s', flexShrink: 0 }} />
              <span style={{ fontSize: 14.5, fontWeight: 600, color: 'var(--snt-text-1)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {activeId ? `Thread · ${String(activeId).substring(0, 8)}` : 'New Conversation'}
              </span>
              {isTyping && <span className="hide-mobile" style={{ fontSize: 12.5, color: '#0058be', fontWeight: 500, whiteSpace: 'nowrap' }}>· AI is responding…</span>}
            </div>
            <div style={{ display: 'flex', gap: 10, flexShrink: 0 }}>
              {activeId && (
                <button className="top-action-btn" onClick={handleNewChat} style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '6px 14px', background: '#ffffff', border: '1px solid #cbd5e1', borderRadius: 8, fontSize: 13, fontWeight: 600, color: '#0f172a', cursor: 'pointer', boxShadow: '0 1px 2px rgba(15,23,42,0.04)', transition: 'all 0.15s ease' }} onMouseEnter={e=>{e.currentTarget.style.background='#f8fafc';e.currentTarget.style.borderColor='#94a3b8'}} onMouseLeave={e=>{e.currentTarget.style.background='#ffffff';e.currentTarget.style.borderColor='#cbd5e1'}}>
                  <span className="material-symbols-outlined" style={{ fontSize: 16 }}>add</span> <span className="hide-mobile">New chat</span>
                </button>
              )}
              <button className="top-action-btn" onClick={() => setIsQuickScanOpen(true)} style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '6px 16px', background: '#0058be', border: 'none', borderRadius: 8, fontSize: 13, fontWeight: 600, color: '#fff', cursor: 'pointer', boxShadow: '0 2px 6px rgba(0,88,190,0.2)' }}>
                <span className="material-symbols-outlined" style={{ fontSize: 16 }}>shield</span> <span className="hide-mobile">Quick Scan</span>
              </button>
            </div>
          </div>

          {/* Messages Area */}
          <div className="chat-main-area chat-body-wrap" style={{ flex: 1, overflowY: 'auto', padding: '32px 32px' }}>
            {/* Welcome State */}
            {isFresh && (
              <div style={{ maxWidth: 840, margin: '0 auto', paddingTop: 20, width: '100%', minWidth: 0 }}>
                <style>{`
                  .sc-sug-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
                  @media (max-width: 640px) {
                    .sc-sug-grid { grid-template-columns: 1fr !important; }
                  }
                `}</style>
                <div style={{
                  background: 'var(--snt-surface)', border: '1px solid rgba(0,88,190,0.1)', borderRadius: 20,
                  padding: '24px 20px', marginBottom: 24,
                  display: 'flex', alignItems: 'flex-start', gap: 16,
                  boxShadow: '0 8px 32px rgba(17,28,45,0.03)'
                }}>
                  <AiAvatar size={52} />
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <h2 className="text-responsive-h2" style={{ margin: '0 0 6px', fontWeight: 700, color: 'var(--snt-text-1)', fontFamily: "'Plus Jakarta Sans', 'Manrope', sans-serif", letterSpacing: '-0.02em' }}>
                      Hello, {user?.firstName || 'there'}. I'm your AI Security Assistant.
                    </h2>
                    <p style={{ margin: 0, fontSize: 14, color: 'var(--snt-text-2)', lineHeight: 1.5 }}>
                      I can explain vulnerabilities from your scan reports, help you understand security findings, suggest remediation strategies, and answer secure coding questions.
                    </p>
                  </div>
                </div>

                {/* Suggestions Grid */}
                <p style={{ margin: '0 0 14px 4px', fontSize: 12, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: '#8890a0' }}>Suggested questions</p>
                <div className="sc-sug-grid">
                  {SUGGESTIONS.map((s, i) => (
                    <button
                      key={i}
                      onClick={() => sendMessage(s.text)}
                      disabled={isTyping}
                      className="suggestion-card"
                      style={{
                        background: 'var(--snt-surface-2)', border: '1px solid #e2e6f0', borderRadius: 14,
                        padding: '14px 18px', textAlign: 'left', cursor: 'pointer',
                        display: 'flex', flexDirection: 'column', gap: 8,
                      }}
                    >
                      <div style={{ width: 32, height: 32, borderRadius: 8, background: '#e8edff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#0058be' }}>{s.icon}</span>
                      </div>
                      <span style={{ fontSize: 13.5, fontWeight: 500, color: 'var(--snt-text-1)', lineHeight: 1.4 }}>{s.text}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Message Thread */}
            {messages.length > 0 && (
              <div style={{ maxWidth: 1000, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 28, width: '100%', minWidth: 0 }}>
                {messages.map((m, idx) => (
                  <div key={m.id || idx} style={{ width: '100%', minWidth: 0 }}>
                    {/* User bubble */}
                    {(m.userMessage || m.attachedFile) && (
                      <div className="msg-bubble-user" style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
                        <div style={{ maxWidth: '85%', minWidth: 0, display: 'flex', flexDirection: 'column', gap: 8, alignItems: 'flex-end' }}>
                          {/* PDF badge */}
                          {m.attachedFile && (
                            <div style={{
                              display: 'flex', alignItems: 'center', gap: 6,
                              background: '#e8edff', borderRadius: 10,
                              padding: '6px 14px', fontSize: 13, color: '#0058be', fontWeight: 600,
                              border: '1px solid #adc6ff',
                            }}>
                              <span className="material-symbols-outlined" style={{ fontSize: 16 }}>picture_as_pdf</span>
                              {m.attachedFile}
                            </div>
                          )}
                          {/* User text bubble */}
                          {m.userMessage && (
                            <div style={{
                              background: '#111c2d', color: '#fff',
                              borderRadius: '20px 20px 4px 20px',
                              padding: '14px 20px', fontSize: 14.5, lineHeight: 1.6,
                              boxShadow: '0 4px 16px rgba(17,28,45,0.08)',
                              whiteSpace: 'pre-wrap',
                            }}>
                              {m.userMessage}
                            </div>
                          )}
                        </div>
                      </div>
                    )}
                    {/* AI bubble */}
                    <div className="msg-bubble-ai" style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                      <div style={{ marginTop: 4, flexShrink: 0 }}><AiAvatar size={34} /></div>
                      <div style={{
                        flex: 1, background: 'var(--snt-surface)', border: 'none',
                        borderRadius: '4px 20px 20px 20px', padding: '18px 24px',
                        boxShadow: '0 4px 24px rgba(17,28,45,0.04), 0 1px 4px rgba(17,28,45,0.02)',
                      }}>
                        {m.aiResponse ? (
                          <div className="prose" style={{ fontSize: 15, color: 'var(--snt-text-1)' }}>
                            <ReactMarkdown remarkPlugins={[remarkGfm]}>
                              {m.aiResponse}
                            </ReactMarkdown>
                          </div>
                        ) : (
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
                            <span className="typing-dot" /><span className="typing-dot" /><span className="typing-dot" />
                            <span style={{ fontSize: 13.5, color: '#8890a0', marginLeft: 6 }}>Generating response…</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
                <div ref={bottomRef} />
              </div>
            )}

            {/* Loading state */}
            {loading && (
              <div style={{ display: 'flex', justifyContent: 'center', padding: 60 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span className="typing-dot" /><span className="typing-dot" /><span className="typing-dot" />
                  <span style={{ fontSize: 14, color: '#8890a0', fontWeight: 500 }}>Loading conversation…</span>
                </div>
              </div>
            )}
          </div>

          {/* ── INPUT AREA ─────────────────────────────── */}
          <div className="chat-input-area" style={{
            flexShrink: 0,
            background: 'var(--snt-surface)',
            padding: '12px 32px 20px',
            borderTop: '1px solid rgba(17,28,45,0.06)',
          }}>
            <div style={{ maxWidth: 1000, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 8 }}>

              {/* Attachment / Scan context chips */}
              {(selectedFile || selectedScanId) && (
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  {selectedFile && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, background: '#f0f3ff', padding: '6px 12px', borderRadius: 8, border: '1px solid #dde2f0' }}>
                      <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#0058be' }}>picture_as_pdf</span>
                      <span style={{ fontSize: 12.5, fontWeight: 600, color: '#0058be', maxWidth: 140, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{selectedFile.name}</span>
                      <button onClick={handleRemoveFile} style={{ border: 'none', background: 'none', cursor: 'pointer', padding: 2, display: 'flex', color: '#0058be' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: 15 }}>close</span>
                      </button>
                    </div>
                  )}
                  {selectedScanId && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, background: '#f0fdf4', border: '1px solid #bbf7d0', padding: '6px 12px', borderRadius: 8 }}>
                      <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#16a34a' }}>shield</span>
                      <span style={{ fontSize: 12.5, fontWeight: 600, color: '#16a34a' }}>Scan #{selectedScanId}</span>
                      <button onClick={() => setSelectedScanId('')} style={{ border: 'none', background: 'none', cursor: 'pointer', padding: 2, display: 'flex', color: '#16a34a' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: 15 }}>close</span>
                      </button>
                    </div>
                  )}
                </div>
              )}

              {/* Quick chips — horizontal scroll on mobile, wrap on desktop */}
              {!selectedFile && (
                <div className="chips-row">
                  {[
                    { label: 'Latest report',       icon: 'description',  msg: 'Explain my latest security report' },
                    { label: 'Review vulnerabilities', icon: 'bug_report', msg: 'Review my recent vulnerabilities' },
                    { label: 'Coding best practices', icon: 'verified_user', msg: 'Secure coding best practices' },
                  ].map(chip => (
                    <button
                      key={chip.label}
                      onClick={() => sendMessage(chip.msg)}
                      disabled={isTyping}
                      className="quick-chip"
                      style={{
                        display: 'flex', alignItems: 'center', gap: 5,
                        padding: '5px 12px', background: 'var(--snt-surface-2)',
                        border: '1px solid #e2e6f0', borderRadius: 99,
                        fontSize: 12.5, fontWeight: 500, color: 'var(--snt-text-2)',
                        cursor: 'pointer', whiteSpace: 'nowrap',
                      }}
                    >
                      <span className="material-symbols-outlined" style={{ fontSize: 14 }}>{chip.icon}</span>
                      {chip.label}
                    </button>
                  ))}
                </div>
              )}

              {/* Input row */}
              <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>

                {/* Scan context button */}
                <div style={{ position: 'relative', flexShrink: 0 }} ref={scanMenuRef}>
                  <button
                    onClick={() => setScanMenuOpen(!scanMenuOpen)}
                    className="chat-action-btn"
                    style={{
                      height: 44, width: 44,
                      background: 'var(--snt-surface-2)', color: 'var(--snt-text-2)',
                      border: '1.5px solid #e2e6f0', borderRadius: 10,
                      cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
                      transition: 'border-color 0.15s, background 0.15s',
                    }}
                    onMouseEnter={e => { e.currentTarget.style.borderColor = '#0058be'; e.currentTarget.style.background = '#f0f3ff'; e.currentTarget.style.color = '#0058be'; }}
                    onMouseLeave={e => { e.currentTarget.style.borderColor = '#e2e6f0'; e.currentTarget.style.background = 'var(--snt-surface-2)'; e.currentTarget.style.color = 'var(--snt-text-2)'; }}
                    title="Select Scan Context"
                  >
                    <span className="material-symbols-outlined" style={{ fontSize: 19 }}>data_object</span>
                  </button>
                  {scanMenuOpen && (
                    <div style={{
                      position: 'absolute', bottom: 52, left: 0, width: 280, maxHeight: 260, overflowY: 'auto',
                      background: 'var(--snt-surface)', border: '1px solid #e2e6f0', borderRadius: 12,
                      boxShadow: '0 -4px 24px rgba(17,28,45,0.08)', zIndex: 200,
                      display: 'flex', flexDirection: 'column', padding: 6,
                    }}>
                      <div style={{ padding: '6px 10px', fontSize: 10.5, fontWeight: 700, color: '#8890a0', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Select Context</div>
                      <button onClick={() => { setSelectedScanId(''); setScanMenuOpen(false); }}
                        style={{ textAlign: 'left', padding: '9px 10px', background: !selectedScanId ? '#f0f3ff' : 'transparent', border: 'none', borderRadius: 8, fontSize: 13, color: !selectedScanId ? '#0058be' : 'var(--snt-text-1)', cursor: 'pointer', fontWeight: !selectedScanId ? 600 : 500 }}
                      >None</button>
                      {scans.map(s => (
                        <button key={s.scanId} onClick={() => { setSelectedScanId(s.scanId); setScanMenuOpen(false); }}
                          style={{ textAlign: 'left', padding: '9px 10px', background: selectedScanId === s.scanId ? '#f0f3ff' : 'transparent', border: 'none', borderRadius: 8, fontSize: 13, color: selectedScanId === s.scanId ? '#0058be' : 'var(--snt-text-1)', cursor: 'pointer', fontWeight: selectedScanId === s.scanId ? 600 : 500, display: 'flex', flexDirection: 'column' }}
                          onMouseEnter={e => { if (selectedScanId !== s.scanId) e.currentTarget.style.background = 'var(--snt-surface-2)' }}
                          onMouseLeave={e => { if (selectedScanId !== s.scanId) e.currentTarget.style.background = 'transparent' }}
                        >
                          <span>{s.scanType === 'QUICK_SCAN' ? 'Quick Scan' : 'Project'} #{s.scanId}</span>
                          <span style={{ fontSize: 10.5, color: selectedScanId === s.scanId ? '#004395' : '#8890a0' }}>Score: {Math.round(s.securityScore || 0)}</span>
                        </button>
                      ))}
                    </div>
                  )}
                </div>

                {/* PDF attach */}
                <input type="file" accept=".pdf" ref={fileInputRef} onChange={handleFileChange} style={{ display: 'none' }} />
                <button
                  onClick={() => fileInputRef.current?.click()}
                  className="chat-action-btn"
                  style={{
                    height: 44, width: 44, flexShrink: 0,
                    background: 'var(--snt-surface-2)', color: 'var(--snt-text-2)',
                    border: '1.5px solid #e2e6f0', borderRadius: 10,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                  onMouseEnter={e => { e.currentTarget.style.background = '#f0f3ff'; e.currentTarget.style.color = '#0058be'; e.currentTarget.style.borderColor = '#0058be'; }}
                  onMouseLeave={e => { e.currentTarget.style.background = 'var(--snt-surface-2)'; e.currentTarget.style.color = 'var(--snt-text-2)'; e.currentTarget.style.borderColor = '#e2e6f0'; }}
                  title="Attach PDF"
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 20 }}>attach_file</span>
                </button>

                {/* Textarea */}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <textarea
                    ref={inputRef}
                    value={input}
                    onChange={e => { setInput(e.target.value); e.target.style.height = 'auto'; e.target.style.height = Math.min(e.target.scrollHeight, 130) + 'px' }}
                    onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(input) } }}
                    disabled={isTyping}
                    placeholder="Ask a security question…"
                    className="chat-input"
                    style={{
                      width: '100%', resize: 'none', minHeight: 44, maxHeight: 130,
                      background: 'var(--snt-surface)', border: '1.5px solid #e2e6f0', borderRadius: 10,
                      padding: '11px 14px', fontSize: 14, color: 'var(--snt-text-1)',
                      lineHeight: 1.5, boxSizing: 'border-box',
                      transition: 'border-color 0.18s, box-shadow 0.18s',
                    }}
                    rows={1}
                  />
                </div>

                {/* Send */}
                <button
                  onClick={() => sendMessage(input)}
                  disabled={(!input.trim() && !selectedFile) || isTyping}
                  className="send-btn send-btn-container"
                  style={{
                    height: 44, width: 44, flexShrink: 0,
                    background: '#0058be', color: '#fff', border: 'none', borderRadius: 10,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
                  }}
                  title="Send"
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 19 }}>send</span>
                </button>
              </div>

              {/* Disclaimer — desktop only */}
              <p className="chat-disclaimer" style={{ margin: 0, fontSize: 11.5, color: '#94a3b8', textAlign: 'center', lineHeight: 1.4 }}>
                AI responses are generated by the platform's language model. Always verify critical security findings manually.
              </p>
            </div>
          </div>
        </main>
      </div>
      <BottomNav />
      <QuickScanModal 
        isOpen={isQuickScanOpen}
        onClose={() => setIsQuickScanOpen(false)}
        onScanComplete={(result) => {
          // You could automatically load the new scan into the chat context here if desired
          fetchScans();
        }}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete Conversation"
        message="This conversation and all its messages will be permanently deleted."
        confirmLabel="Delete"
        variant="danger"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  )
}
