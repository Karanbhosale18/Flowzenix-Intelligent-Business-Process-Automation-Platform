import { NavLink, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import AuthService from '../services/AuthService.js'
import './AppShell.css'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', icon: '\u25A6' },
  { to: '/requests/new', label: 'New request', icon: '\u2795' },
  { to: '/requests', label: 'My requests', icon: '\u2630' },
  { to: '/approvals', label: 'Approvals', icon: '\u2713' },
]

export default function AppShell({ children, title, actions }) {
  const navigate = useNavigate()
  const user = AuthService.getCurrentUser()
  const [theme, setTheme] = useState(() => localStorage.getItem('theme') || 'dark')

  useEffect(() => {
    document.documentElement.dataset.theme = theme
  }, [theme])

  function handleLogout() {
    AuthService.logout()
    navigate('/login')
  }

  function toggleTheme() {
    const nextTheme = theme === 'dark' ? 'light' : 'dark'
    localStorage.setItem('theme', nextTheme)
    setTheme(nextTheme)
  }

  return (
    <div className="shell">
      <aside className="shell-sidebar">
        <div className="shell-brand">
          <span className="shell-brand-dot" />
          FlowGate
        </div>

        <nav className="shell-nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `shell-nav-link${isActive ? ' shell-nav-link--active' : ''}`}
            >
              <span className="shell-nav-icon" aria-hidden="true">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
          <NavLink to="/profile" className={({ isActive }) => `shell-nav-link${isActive ? ' shell-nav-link--active' : ''}`}>
            <span className="shell-nav-icon" aria-hidden="true">◉</span>
            My profile
          </NavLink>
          {(user?.roles || []).includes('ROLE_ADMIN') && (
            <NavLink to="/admin" className={({ isActive }) => `shell-nav-link${isActive ? ' shell-nav-link--active' : ''}`}>
              <span className="shell-nav-icon" aria-hidden="true">⚙</span>
              Admin
            </NavLink>
          )}
        </nav>

        <div className="shell-user">
          <div className="shell-user-name">{user?.username}</div>
          <div className="shell-user-roles">{(user?.roles || []).map((r) => r.replace('ROLE_', '')).join(', ')}</div>
          <button className="shell-theme" onClick={toggleTheme} aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}>
            {theme === 'dark' ? '☀ Light mode' : '◐ Dark mode'}
          </button>
          <button className="shell-logout" onClick={handleLogout}>Sign out</button>
        </div>
      </aside>

      <main className="shell-main">
        <header className="shell-header">
          <h1>{title}</h1>
          {actions && <div className="shell-header-actions">{actions}</div>}
        </header>
        <div className="shell-content">{children}</div>
      </main>
    </div>
  )
}
