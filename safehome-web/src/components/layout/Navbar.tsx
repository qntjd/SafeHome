import { useState, useRef, useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/store/authStore'

const NAV_ITEMS = [
  { path: '/',         label: '홈', icon: '🏠' },
  { path: '/map',      label: '안전 지도', icon: '🗺' },
  { path: '/trip',     label: '안심 귀가', icon: '🚶' },
  { path: '/news',     label: '안전 뉴스', icon: '📰' },
  { path: '/crime',    label: '범죄 통계', icon: '📊' },
  { path: '/resources',  label: '개인 정보 보호',    icon: '🛡' },
]

export default function Navbar() {
  const { pathname }              = useLocation()
  const { handleLogout, nickname } = useAuth()
  const { login, email, accessToken, refreshToken } = useAuthStore()
  const queryClient               = useQueryClient()

  const [menuOpen,    setMenuOpen]    = useState(false)
  const [dropOpen,    setDropOpen]    = useState(false)
  const [editMode,    setEditMode]    = useState(false)
  const [newNickname, setNewNickname] = useState(nickname ?? '')

  const dropRef = useRef<HTMLDivElement>(null)

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropRef.current && !dropRef.current.contains(e.target as Node)) {
        setDropOpen(false)
        setEditMode(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const updateMutation = useMutation({
    mutationFn: () => userApi.updateProfile(newNickname),
    onSuccess: ({ data }) => {
      // 스토어 닉네임 업데이트
      login({
        accessToken:  accessToken ?? '',
        refreshToken: refreshToken ?? '',
        email:        email ?? '',
        nickname:     data.data.nickname,
      })
      queryClient.invalidateQueries({ queryKey: ['me'] })
      setEditMode(false)
      setDropOpen(false)
    },
    onError: () => alert('닉네임 변경에 실패했습니다.'),
  })

  return (
    <>
      <nav
        className="h-14 flex items-center justify-between px-4 sm:px-6 relative z-40"
        style={{ background: 'var(--bg-secondary)', borderBottom: '1px solid var(--border)' }}
      >
        {/* 로고 */}
        <div className="flex items-center gap-8">
          <Link to="/" className="flex items-center gap-2">
            <div
              className="w-7 h-7 rounded-lg flex items-center justify-center text-sm font-bold"
              style={{ background: 'var(--accent-blue)' }}
            >
              S
            </div>
            <span className="font-semibold text-base" style={{ color: 'var(--text-primary)' }}>
              SafeHome
            </span>
          </Link>

          {/* 데스크탑 메뉴 */}
          <div className="hidden sm:flex items-center gap-1">
            {NAV_ITEMS.map(({ path, label }) => (
              <Link
                key={path}
                to={path}
                className="px-3 py-1.5 rounded-lg text-sm transition-all"
                style={{
                  color:      pathname === path ? 'var(--accent-blue)' : 'var(--text-secondary)',
                  background: pathname === path ? 'rgba(79,126,248,0.1)' : 'transparent',
                  fontWeight: pathname === path ? 500 : 400,
                }}
              >
                {label}
              </Link>
            ))}
          </div>
        </div>

        {/* 데스크탑 우측 — 닉네임 드롭다운 */}
        <div className="hidden sm:flex items-center gap-3 relative" ref={dropRef}>
          <button
            onClick={() => { setDropOpen(!dropOpen); setEditMode(false) }}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm transition-all"
            style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', color: 'var(--text-secondary)' }}
          >
            <div
              className="w-5 h-5 rounded-full flex items-center justify-center text-xs font-medium"
              style={{ background: 'var(--accent-blue)', color: '#fff' }}
            >
              {nickname?.[0]?.toUpperCase()}
            </div>
            {nickname}
            <span style={{ fontSize: 10, opacity: 0.5 }}>▼</span>
          </button>

          {/* 드롭다운 메뉴 */}
          {dropOpen && (
            <div
              className="absolute top-10 right-0 w-64 rounded-xl shadow-xl py-2 z-50"
              style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
            >
              {/* 프로필 헤더 */}
              <div
                className="px-4 py-3 mb-1"
                style={{ borderBottom: '1px solid var(--border)' }}
              >
                <p className="text-xs mb-0.5" style={{ color: 'var(--text-muted)' }}>
                  {email}
                </p>
                <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                  {nickname}
                </p>
              </div>

              {/* 닉네임 변경 */}
              {editMode ? (
                <div className="px-4 py-3" style={{ borderBottom: '1px solid var(--border)' }}>
                  <p className="text-xs mb-2" style={{ color: 'var(--text-muted)' }}>
                    닉네임 변경
                  </p>
                  <input
                    value={newNickname}
                    onChange={(e) => setNewNickname(e.target.value)}
                    className="w-full rounded-lg px-3 py-2 text-sm outline-none mb-2"
                    style={{
                      background: 'var(--bg-hover)',
                      border: '1px solid var(--border)',
                      color: 'var(--text-primary)',
                    }}
                    autoFocus
                    onKeyDown={(e) => e.key === 'Enter' && updateMutation.mutate()}
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={() => updateMutation.mutate()}
                      disabled={updateMutation.isPending || !newNickname.trim()}
                      className="flex-1 rounded-lg py-1.5 text-xs font-medium transition-all disabled:opacity-50"
                      style={{ background: 'var(--accent-blue)', color: '#fff' }}
                    >
                      {updateMutation.isPending ? '저장 중...' : '저장'}
                    </button>
                    <button
                      onClick={() => { setEditMode(false); setNewNickname(nickname ?? '') }}
                      className="flex-1 rounded-lg py-1.5 text-xs transition-all"
                      style={{ border: '1px solid var(--border)', color: 'var(--text-muted)' }}
                    >
                      취소
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  onClick={() => { setEditMode(true); setNewNickname(nickname ?? '') }}
                  className="w-full text-left px-4 py-2.5 text-sm transition-colors flex items-center gap-3"
                  style={{ color: 'var(--text-secondary)' }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                >
                  <span>✏️</span>
                  닉네임 변경
                </button>
              )}

              {/* 설정 메뉴 */}
              <Link
                to="/settings"
                onClick={() => setDropOpen(false)}
                className="w-full text-left px-4 py-2.5 text-sm transition-colors flex items-center gap-3"
                style={{ color: 'var(--text-secondary)', display: 'flex' }}
                onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
              >
                <span>⚙️</span>
                설정 (비상연락처)
              </Link>

              {/* 로그아웃 */}
              <div style={{ borderTop: '1px solid var(--border)', marginTop: 4 }}>
                <button
                  onClick={handleLogout}
                  className="w-full text-left px-4 py-2.5 text-sm transition-colors flex items-center gap-3 mt-1"
                  style={{ color: 'var(--accent-red)' }}
                  onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                  onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                >
                  <span>🚪</span>
                  로그아웃
                </button>
              </div>
            </div>
          )}
        </div>

        {/* 모바일 햄버거 */}
        <button
          onClick={() => setMenuOpen(!menuOpen)}
          className="sm:hidden flex flex-col gap-1.5 p-2"
        >
          <span
            className={`block w-5 h-0.5 transition-all ${menuOpen ? 'rotate-45 translate-y-2' : ''}`}
            style={{ background: 'var(--text-secondary)' }}
          />
          <span
            className={`block w-5 h-0.5 transition-all ${menuOpen ? 'opacity-0' : ''}`}
            style={{ background: 'var(--text-secondary)' }}
          />
          <span
            className={`block w-5 h-0.5 transition-all ${menuOpen ? '-rotate-45 -translate-y-2' : ''}`}
            style={{ background: 'var(--text-secondary)' }}
          />
        </button>
      </nav>

      {/* 모바일 드롭다운 */}
      {menuOpen && (
        <div
          className="sm:hidden fixed top-14 left-0 right-0 z-50 py-2"
          style={{ background: 'var(--bg-secondary)', borderBottom: '1px solid var(--border)' }}
        >
          {/* 프로필 */}
          <div
            className="px-4 py-3 mb-1"
            style={{ borderBottom: '1px solid var(--border)' }}
          >
            <div className="flex items-center gap-3">
              <div
                className="w-9 h-9 rounded-full flex items-center justify-center font-medium"
                style={{ background: 'var(--accent-blue)', color: '#fff' }}
              >
                {nickname?.[0]?.toUpperCase()}
              </div>
              <div>
                <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                  {nickname}
                </p>
                <p className="text-xs" style={{ color: 'var(--text-muted)' }}>{email}</p>
              </div>
            </div>
          </div>

          {NAV_ITEMS.map(({ path, label, icon }) => (
            <Link
              key={path}
              to={path}
              onClick={() => setMenuOpen(false)}
              className="flex items-center gap-3 px-4 py-3 text-sm transition-colors"
              style={{
                color:      pathname === path ? 'var(--accent-blue)' : 'var(--text-secondary)',
                background: pathname === path ? 'rgba(79,126,248,0.08)' : 'transparent',
              }}
            >
              <span>{icon}</span>
              {label}
            </Link>
          ))}

          <div style={{ borderTop: '1px solid var(--border)', marginTop: 4 }}>
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-3 px-4 py-3 text-sm mt-1"
              style={{ color: 'var(--accent-red)' }}
            >
              <span>🚪</span>
              로그아웃
            </button>
          </div>
        </div>
      )}

      {/* 모바일 하단 탭바 */}
      <div
        className="sm:hidden fixed bottom-0 left-0 right-0 z-40 flex"
        style={{
          background: 'var(--bg-secondary)',
          borderTop: '1px solid var(--border)',
          paddingBottom: 'env(safe-area-inset-bottom)',
        }}
      >
        {NAV_ITEMS.map(({ path, label, icon }) => (
          <Link
            key={path}
            to={path}
            className="flex-1 flex flex-col items-center justify-center py-2 gap-0.5 text-xs transition-colors"
            style={{ color: pathname === path ? 'var(--accent-blue)' : 'var(--text-muted)' }}
          >
            <span style={{ fontSize: 18 }}>{icon}</span>
            <span>{label}</span>
          </Link>
        ))}
      </div>
    </>
  )
}