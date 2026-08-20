import { useNavigate } from 'react-router-dom'
import AuthService from '../services/AuthService.js'
import './Dashboard.css'

export default function Dashboard() {
  const navigate = useNavigate()
  const user = AuthService.getCurrentUser()

  function handleLogout() {
    AuthService.logout()
    navigate('/login')
  }

  return (
    <div className="dash-shell">
      <div className="dash-card">
        <div className="dash-dot" />
        <h1>You're signed in</h1>
        <p className="dash-user">
          {user?.username} · {user?.email}
        </p>
        <p className="dash-roles">Roles: {user?.roles?.join(', ') || 'ROLE_USER'}</p>
        <p className="dash-note">
          This page is only reachable with a valid JWT — remove or replace it with your
          real approval-queue UI once the workflow modules are built.
        </p>
        <button className="dash-logout" onClick={handleLogout}>Sign out</button>
      </div>
    </div>
  )
}
