import React, { useState, useRef, useEffect } from 'react'
import api from '../api/axios.js'

export default function AiChatWidget() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState([
    { role: 'assistant', text: 'Ask me about the team\'s recent work — e.g. "What blockers came up last week?"' }
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const scrollRef = useRef(null)

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages, loading])

  async function send() {
    const question = input.trim()
    if (!question || loading) return
    setMessages((m) => [...m, { role: 'user', text: question }])
    setInput('')
    setLoading(true)
    try {
      const { data } = await api.post('/api/ai/chat', { question })
      setMessages((m) => [...m, { role: 'assistant', text: data.answer }])
    } catch (e) {
      setMessages((m) => [...m, { role: 'assistant', text: 'Sorry, the assistant is unavailable right now.' }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {open && (
        <div className="chat-panel">
          <div className="chat-panel-header">
            <span>Team Insights Assistant</span>
            <button onClick={() => setOpen(false)}>×</button>
          </div>
          <div className="chat-messages" ref={scrollRef}>
            {messages.map((m, i) => (
              <div key={i} className={`chat-msg ${m.role}`}>{m.text}</div>
            ))}
            {loading && <div className="loading-dots">Thinking…</div>}
          </div>
          <div className="chat-input-row">
            <input
              placeholder="Ask about the team's week…"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && send()}
            />
            <button className="btn btn-accent" onClick={send} disabled={loading}>Send</button>
          </div>
        </div>
      )}
      <button className="chat-fab" onClick={() => setOpen((o) => !o)}>
        {open ? 'Close chat' : '✦ Ask AI'}
      </button>
    </>
  )
}
