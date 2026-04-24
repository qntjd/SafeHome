import { useForm } from 'react-hook-form'
import { useAuth } from '@/hooks/useAuth'
import { Link } from 'react-router-dom'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import { useState } from 'react'

interface FormData {
  email: string
  password: string
  nickname: string
}

export default function RegisterPage() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>()
  const { handleRegister } = useAuth()
  const { position } = useCurrentLocation()
  const [error, setError] = useState('')

  const onSubmit = async ({ email, password, nickname }: FormData) => {
    try {
      setError('')
      await handleRegister(email, password, nickname, position.lat, position.lng)
    } catch {
      setError('회원가입에 실패했습니다. 이미 사용 중인 이메일일 수 있어요.')
    }
  }

  const inputStyle = {
    background: 'var(--bg-hover)',
    border: '1px solid var(--border)',
    color: 'var(--text-primary)',
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center px-4"
      style={{ background: 'var(--bg-primary)' }}
    >
      <div
        className="fixed top-0 left-1/2 -translate-x-1/2 w-96 h-96 rounded-full opacity-10 blur-3xl pointer-events-none"
        style={{ background: 'var(--accent-blue)' }}
      />

      <div className="w-full max-w-sm relative">
        <div className="text-center mb-8">
          <div
            className="w-12 h-12 rounded-2xl flex items-center justify-center text-xl font-bold mx-auto mb-4"
            style={{ background: 'var(--accent-blue)' }}
          >
            S
          </div>
          <h1 className="text-2xl font-bold" style={{ color: 'var(--text-primary)' }}>
            회원가입
          </h1>
          <p className="text-sm mt-1" style={{ color: 'var(--text-muted)' }}>
            현재 위치가 집 위치로 자동 등록됩니다.
          </p>
        </div>

        <div
          className="rounded-2xl p-6"
          style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
        >
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <div>
              <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                이메일
              </label>
              <input
                type="email"
                placeholder="email@example.com"
                className="w-full rounded-xl px-4 py-3 text-sm outline-none transition-all focus:ring-2 focus:ring-blue-500"
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
              <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                닉네임
              </label>
              <input
                placeholder="홍길동"
                className="w-full rounded-xl px-4 py-3 text-sm outline-none transition-all focus:ring-2 focus:ring-blue-500"
                style={inputStyle}
                {...register('nickname', { required: '닉네임을 입력해주세요.' })}
              />
              {errors.nickname && (
                <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>
                  {errors.nickname.message}
                </p>
              )}
            </div>

            <div>
              <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                비밀번호
              </label>
              <input
                type="password"
                placeholder="8자 이상 입력해주세요"
                className="w-full rounded-xl px-4 py-3 text-sm outline-none transition-all focus:ring-2 focus:ring-blue-500"
                style={inputStyle}
                {...register('password', {
                  required: '비밀번호를 입력해주세요.',
                  minLength: { value: 8, message: '8자 이상 입력해주세요.' },
                })}
              />
              {errors.password && (
                <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>
                  {errors.password.message}
                </p>
              )}
            </div>

            {error && (
              <div
                className="rounded-xl px-4 py-3 text-sm"
                style={{
                  background: 'rgba(248,113,113,0.1)',
                  color: 'var(--accent-red)',
                  border: '1px solid rgba(248,113,113,0.2)',
                }}
              >
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-xl py-3 text-sm font-medium transition-all disabled:opacity-50"
              style={{ background: 'var(--accent-blue)', color: '#fff' }}
            >
              {isSubmitting ? '가입 중...' : '가입하기'}
            </button>
          </form>

          <p className="text-center text-sm mt-4" style={{ color: 'var(--text-muted)' }}>
            이미 계정이 있으신가요?{' '}
            <Link to="/login" style={{ color: 'var(--accent-blue)' }}>
              로그인
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}