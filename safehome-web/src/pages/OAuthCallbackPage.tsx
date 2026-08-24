import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

export default function OAuthCallbackPage() {
  const [params] = useSearchParams()
  const navigate  = useNavigate()
  const { login } = useAuthStore()

  useEffect(() => {
    const accessToken  = params.get('accessToken')
    const refreshToken = params.get('refreshToken')
    const nickname     = params.get('nickname')
    const email        = params.get('email') ?? ''


    if (accessToken && refreshToken && nickname) {
      login({
        accessToken,
        refreshToken,
        email,
        nickname,
      })
      navigate('/', { replace: true })
    } else {
      navigate('/login', { replace: true })
    }
  }, [])

  return (
    <div
      className="min-h-screen flex items-center justify-center"
      style={{ background: 'var(--bg-primary)' }}
    >
      <div className="text-center">
        <div
          className="w-14 h-14 rounded-2xl flex items-center justify-center mx-auto mb-6 relative"
          style={{ background: 'var(--accent-amber)', boxShadow: 'var(--shadow-beacon)' }}
        >
          <span className="beacon-dot" style={{ position: 'absolute', top: -3, right: -3, background: 'var(--grade-a)', boxShadow: '0 0 0 3px var(--bg-primary)' }} />
          <span className="font-display font-black" style={{ color: 'var(--ink)', fontSize: 22 }}>S</span>
        </div>
        <div
          className="w-8 h-8 rounded-full border-2 border-t-transparent animate-spin mx-auto mb-4"
          style={{ borderColor: 'var(--accent-amber)', borderTopColor: 'transparent' }}
        />
        <p className="text-sm font-display font-semibold" style={{ color: 'var(--text-primary)' }}>
          로그인 처리 중...
        </p>
        <p className="text-xs mt-1 font-mono" style={{ color: 'var(--text-muted)' }}>
          잠시만 기다려주세요
        </p>
      </div>
    </div>
  )
}