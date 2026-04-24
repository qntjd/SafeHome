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
          className="w-10 h-10 rounded-full border-2 border-t-transparent animate-spin mx-auto mb-4"
          style={{ borderColor: 'var(--accent-blue)', borderTopColor: 'transparent' }}
        />
        <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
          로그인 처리 중...
        </p>
      </div>
    </div>
  )
}