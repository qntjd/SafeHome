import { useForm } from 'react-hook-form'
import { useAuth } from '@/hooks/useAuth'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { Map, Footprints, Bell, Newspaper } from 'lucide-react'

interface FormData { email: string; password: string }

export default function LoginPage() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>()
  const { handleLogin } = useAuth()
  const [error, setError] = useState('')

  const onSubmit = async ({ email, password }: FormData) => {
    try {
      setError('')
      await handleLogin(email, password)
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.')
    }
  }

  const inputStyle = {
    background: 'var(--bg-hover)',
    border: '1px solid var(--border)',
    color: 'var(--text-primary)',
  }

  return (
    <div className="min-h-screen flex" style={{ background: 'var(--bg-primary)' }}>
      {/* 왼쪽 잉크 히어로 패널 (데스크탑) */}
      <div
        className="hidden sm:flex flex-col justify-center items-center flex-1 px-12 relative overflow-hidden"
        style={{ background: 'var(--ink)' }}
      >
        <div
          className="absolute rounded-full"
          style={{ width: 340, height: 340, left: -100, top: -140, background: 'radial-gradient(circle, rgba(232,163,61,0.16) 0%, transparent 70%)' }}
        />
        <div
          className="absolute rounded-full"
          style={{ width: 260, height: 260, right: -80, bottom: -120, background: 'radial-gradient(circle, rgba(11,110,130,0.22) 0%, transparent 70%)' }}
        />
        <div className="max-w-sm text-center relative">
          <div
            className="w-16 h-16 rounded-2xl mx-auto mb-6 flex items-center justify-center relative"
            style={{ background: 'var(--accent-amber)', boxShadow: 'var(--shadow-beacon)' }}
          >
            <span className="beacon-dot beacon-dot--static" style={{ position: 'absolute', top: -3, right: -3, background: 'var(--grade-a)', boxShadow: '0 0 0 3px var(--ink)' }} />
            <span className="font-display font-black" style={{ color: 'var(--ink)', fontSize: 28 }}>S</span>
          </div>
          <h1 className="font-display font-black text-3xl mb-3" style={{ color: 'var(--ink-text)' }}>SafeHome</h1>
          <p className="text-base flex items-center justify-center gap-2" style={{ color: 'var(--ink-text-muted)' }}>
            <span className="beacon-dot" />
            1인 가구를 위한 안심 생활 플랫폼
          </p>
          <div className="mt-10 flex flex-col gap-3 text-left">
            {[
              { icon: Map, text: '동네 안전점수 지도' },
              { icon: Footprints, text: '안심 귀가 & 워치독' },
              { icon: Bell, text: '실시간 재난 알림' },
              { icon: Newspaper, text: '안전 뉴스 피드' },
            ].map(({ icon: Icon, text }) => (
              <div key={text} className="flex items-center gap-3 px-4 py-3 rounded-xl"
                style={{ background: 'var(--ink-2)', border: '1px solid var(--ink-border)' }}>
                <Icon size={18} strokeWidth={2} color="var(--accent-amber)" />
                <span className="text-sm font-medium" style={{ color: 'var(--ink-text)' }}>{text}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 오른쪽 로그인 폼 */}
      <div className="flex-1 sm:max-w-md flex flex-col justify-center px-6 sm:px-12"
        style={{ background: 'var(--bg-card)' }}>
        <div className="sm:hidden text-center mb-8">
          <div className="w-12 h-12 rounded-2xl mx-auto mb-3 flex items-center justify-center"
            style={{ background: 'var(--accent-amber)' }}>
            <span className="font-display font-black text-lg" style={{ color: 'var(--ink)' }}>S</span>
          </div>
          <h1 className="font-display font-extrabold text-xl" style={{ color: 'var(--text-primary)' }}>SafeHome</h1>
        </div>

        <h2 className="font-display font-black text-2xl mb-2" style={{ color: 'var(--text-primary)' }}>로그인</h2>
        <p className="text-sm mb-8" style={{ color: 'var(--text-muted)' }}>
          계정에 로그인하여 서비스를 이용하세요
        </p>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div>
            <label className="text-xs font-semibold block mb-1.5"
              style={{ color: 'var(--text-secondary)' }}>이메일</label>
            <input
              type="email"
              placeholder="email@example.com"
              className="w-full rounded-xl px-4 py-3 text-sm outline-none transition-all focus:ring-2 focus:ring-[var(--accent-amber)]"
              style={inputStyle}
              {...register('email', { required: '이메일을 입력해주세요.' })}
            />

            {errors.email && (
              <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>
                {errors.email.message}
              </p>
            )}
          </div>

          <div>
            <label className="text-xs font-semibold block mb-1.5"
              style={{ color: 'var(--text-secondary)' }}>비밀번호</label>
            <input
              type="password"
              placeholder="••••••••"
              className="w-full rounded-xl px-4 py-3 text-sm outline-none transition-all focus:ring-2 focus:ring-[var(--accent-amber)]"
              style={inputStyle}
              {...register('password', { required: '비밀번호를 입력해주세요.' })}
            />
            {errors.password && (
              <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>
                {errors.password.message}
              </p>
            )}
          </div>

          {error && (
            <div className="rounded-xl px-4 py-3 text-sm"
              style={{ background: 'var(--accent-red-soft)', color: 'var(--accent-red)' }}>
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-xl py-3 text-sm font-display font-bold transition-all disabled:opacity-60 mt-2"
            style={{ background: 'var(--accent-amber)', color: 'var(--ink)' }}
          >
            {isSubmitting ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <p className="text-center text-sm mt-6" style={{ color: 'var(--text-muted)' }}>
          계정이 없으신가요?{' '}
          <Link to="/register" style={{ color: 'var(--accent-blue)', fontWeight: 600 }}>
            회원가입
          </Link>
        </p>
        {/* 구분선 */}
        <div className="flex items-center gap-3 my-2">
          <div className="flex-1 h-px" style={{ background: 'var(--border)' }} />
          <span className="text-xs" style={{ color: 'var(--text-muted)' }}>또는</span>
          <div className="flex-1 h-px" style={{ background: 'var(--border)' }} />
        </div>

        {/* 구글 로그인 버튼 */}
        <a
          href="/oauth2/authorization/google"
          className="flex items-center justify-center gap-3 w-full rounded-xl py-3 text-sm font-medium transition-all"
          style={{
            background: 'var(--bg-hover)',
            border: '1px solid var(--border)',
            color: 'var(--text-primary)',
          }}
        >
          <svg width="18" height="18" viewBox="0 0 18 18">
            <path fill="#4285F4" d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.874 2.684-6.615z"/>
            <path fill="#34A853" d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18z"/>
            <path fill="#FBBC05" d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332z"/>
            <path fill="#EA4335" d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 6.29C4.672 4.163 6.656 3.58 9 3.58z"/>
          </svg>
          Google로 로그인
        </a>
      </div>
    </div>
  )
}
