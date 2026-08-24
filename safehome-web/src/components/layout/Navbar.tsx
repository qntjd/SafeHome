import { useState, useRef, useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/store/authStore'
import { Home, Map, Footprints, Newspaper, BarChart3, ShieldCheck, Pencil, Settings, LogOut } from 'lucide-react'

const NAV_ITEMS = [
  { path: '/',         label: '홈', icon: Home },
  { path: '/map',      label: '안전 지도', icon: Map },
  { path: '/trip',     label: '안심 귀가', icon: Footprints },
  { path: '/news',     label: '안전 뉴스', icon: Newspaper },
  { path: '/crime',    label: '범죄 통계', icon: BarChart3 },
  { path: '/resources',  label: '개인 정보 보호',    icon: ShieldCheck },
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
        className="h-16 flex items-center justify-between px-4 sm:px-6 relative z-40"
        style={{ background: 'var(--ink)', borderBottom: '1px solid var(--ink-border)' }}
      >
        {/* 로고 */}
        <div className="flex items-center gap-8">
          <Link to="/" className="flex items-center gap-2.5">
            <div
              className="w-8 h-8 rounded-full flex items-center justify-center relative"
              style={{ background: 'var(--accent-amber)' }}
            >
              <span className="beacon-dot beacon-dot--static" style={{ position: 'absolute', top: -2, right: -2, background: 'var(--grade-a)', boxShadow: '0 0 0 2px var(--ink)' }} />
              <span className="font-display font-black text-sm" style={{ color: 'var(--ink)' }}>S</span>
            </div>
            <span className="font-display font-extrabold text-lg tracking-tight" style={{ color: 'var(--ink-text)' }}>
              SafeHome
            </span>
          </Link>

          {/* 데스크탑 메뉴 */}
          <div className="hidden sm:flex items-center gap-1">
            {NAV_ITEMS.map(({ path, label }) => {
              const active = pathname === path
              return (
                <Link
                  key={path}
                  to={path}
                  className="relative px-3 py-2 text-sm transition-colors flex items-center gap-1.5"
                  style={{
                    color: active ? 'var(--ink-text)' : 'var(--ink-text-muted)',
                    fontWeight: active ? 600 : 500,
                  }}
                >
                  {active && <span className="beacon-dot" />}
                  {label}
                  {active && (
                    <span
                      className="absolute left-3 right-3 -bottom-px h-0.5 rounded-full"
                      style={{ background: 'var(--accent-amber)' }}
                    />
                  )}
                </Link>
              )
            })}
          </div>
        </div>

        {/* 데스크탑 우측 — 닉네임 드롭다운 */}
        <div className="hidden sm:flex items-center gap-3 relative" ref={dropRef}>
          <button
            onClick={() => { setDropOpen(!dropOpen); setEditMode(false) }}
            className="flex items-center gap-2 px-3 py-1.5 rounded-full text-sm transition-all"
            style={{ background: 'var(--ink-2)', border: '1px solid var(--ink-border)', color: 'var(--ink-text-muted)' }}
          >
            <div
              className="w-5 h-5 rounded-full flex items-center justify-center text-xs font-medium"
              style={{ background: 'var(--accent-amber)', color: 'var(--ink)' }}
            >
              {nickname?.[0]?.toUpperCase()}
            </div>
            {nickname}
            <span style={{ fontSize: 10, opacity: 0.6 }}>▾</span>
          </button>

          {/* 드롭다운 메뉴 */}
          {dropOpen && (
            <div
              className="absolute top-12 right-0 w-64 rounded-2xl py-2 z-50"
              style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', boxShadow: 'var(--shadow-md)' }}
            >
              {/* 프로필 헤더 */}
              <div
                className="px-4 py-3 mb-1"
                style={{ borderBottom: '1px solid var(--border)' }}
              >
                <p className="text-xs mb-0.5" style={{ color: 'var(--text-muted)' }}>
                  {email}
                </p>
                <p className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
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
                      className="flex-1 rounded-lg py-1.5 text-xs font-semibold transition-all disabled:opacity-50"
                      style={{ background: 'var(--accent-amber)', color: 'var(--ink)' }}
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
                  <Pencil size={15} strokeWidth={2} />
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
                <Settings size={15} strokeWidth={2} />
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
                  <LogOut size={15} strokeWidth={2} />
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
            style={{ background: 'var(--ink-text-muted)' }}
          />
          <span
            className={`block w-5 h-0.5 transition-all ${menuOpen ? 'opacity-0' : ''}`}
            style={{ background: 'var(--ink-text-muted)' }}
          />
          <span
            className={`block w-5 h-0.5 transition-all ${menuOpen ? '-rotate-45 -translate-y-2' : ''}`}
            style={{ background: 'var(--ink-text-muted)' }}
          />
        </button>
      </nav>

      {/* 모바일 드롭다운 */}
      {menuOpen && (
        <div
          className="sm:hidden fixed top-16 left-0 right-0 z-50 py-2"
          style={{ background: 'var(--ink)', borderBottom: '1px solid var(--ink-border)' }}
        >
          {/* 프로필 */}
          <div
            className="px-4 py-3 mb-1"
            style={{ borderBottom: '1px solid var(--ink-border)' }}
          >
            <div className="flex items-center gap-3">
              <div
                className="w-9 h-9 rounded-full flex items-center justify-center font-semibold"
                style={{ background: 'var(--accent-amber)', color: 'var(--ink)' }}
              >
                {nickname?.[0]?.toUpperCase()}
              </div>
              <div>
                <p className="text-sm font-medium" style={{ color: 'var(--ink-text)' }}>
                  {nickname}
                </p>
                <p className="text-xs" style={{ color: 'var(--ink-text-muted)' }}>{email}</p>
              </div>
            </div>
          </div>

          {NAV_ITEMS.map(({ path, label, icon: Icon }) => (
            <Link
              key={path}
              to={path}
              onClick={() => setMenuOpen(false)}
              className="flex items-center gap-3 px-4 py-3 text-sm transition-colors"
              style={{
                color:      pathname === path ? 'var(--accent-amber)' : 'var(--ink-text-muted)',
                background: pathname === path ? 'var(--ink-2)' : 'transparent',
              }}
            >
              <Icon size={17} strokeWidth={2} />
              {label}
            </Link>
          ))}

          <div style={{ borderTop: '1px solid var(--ink-border)', marginTop: 4 }}>
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-3 px-4 py-3 text-sm mt-1"
              style={{ color: 'var(--accent-red)' }}
            >
              <LogOut size={17} strokeWidth={2} />
              로그아웃
            </button>
          </div>
        </div>
      )}

      {/* 모바일 하단 탭바 */}
      <div
        className="sm:hidden fixed bottom-0 left-0 right-0 z-40 flex"
        style={{
          background: 'var(--ink)',
          borderTop: '1px solid var(--ink-border)',
          paddingBottom: 'env(safe-area-inset-bottom)',
        }}
      >
        {NAV_ITEMS.map(({ path, label, icon: Icon }) => {
          const active = pathname === path
          return (
            <Link
              key={path}
              to={path}
              className="flex-1 flex flex-col items-center justify-center py-2 gap-0.5 text-xs transition-colors relative"
              style={{ color: active ? 'var(--accent-amber)' : 'var(--ink-text-muted)' }}
            >
              {active && (
                <span
                  className="absolute top-0 left-1/2 -translate-x-1/2 w-8 h-0.5 rounded-full"
                  style={{ background: 'var(--accent-amber)' }}
                />
              )}
              <Icon size={19} strokeWidth={2} />
              <span>{label}</span>
            </Link>
          )
        })}
      </div>
    </>
  )
}
