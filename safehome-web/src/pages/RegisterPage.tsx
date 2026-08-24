import { useForm } from 'react-hook-form'
import { useAuth } from '@/hooks/useAuth'
import { Link } from 'react-router-dom'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import { useState } from 'react'
import api from '@/api/axios'
import { CheckCircle2 } from 'lucide-react'

interface FormData {
  email: string
  password: string
  nickname: string
}

export default function RegisterPage() {
  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm<FormData>()
  const { handleRegister } = useAuth()
  const { position } = useCurrentLocation()
  const [error, setError] = useState('')
  const [codeSent, setCodeSent] = useState(false)
  const [verifyCode, setVerifyCode] = useState('')
  const [isVerified, setIsVerified] = useState(false)
  const [sending, setSending] = useState(false)
  const [verifying, setVerifying] = useState(false)

  const email = watch('email')

  const sendCode = async () => {
    if (!email) return setError('이메일을 입력해주세요.')
    setSending(true)
    try {
      await api.post('/auth/email/send', { email })
      setCodeSent(true)
      setError('')
    } catch {
      setError('인증코드 발송에 실패했습니다.')
    } finally {
      setSending(false)
    }
  }

  const verifyEmail = async () => {
    if (!verifyCode) return setError('인증코드를 입력해주세요.')
    setVerifying(true)
    try {
      const res = await api.post('/auth/email/verify', { email, code: verifyCode })
      if (res.data?.data) {
        setIsVerified(true)
        setError('')
      } else {
        setError('인증코드가 올바르지 않습니다.')
      }
    } catch {
      setError('인증 확인에 실패했습니다.')
    } finally {
      setVerifying(false)
    }
  }

  const onSubmit = async ({ email, password, nickname }: FormData) => {
    if (!isVerified) return setError('이메일 인증을 완료해주세요.')
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
      className="min-h-screen flex items-center justify-center px-4 py-10"
      style={{ background: 'var(--bg-primary)' }}
    >
      <div
        className="fixed top-0 left-1/2 -translate-x-1/2 w-96 h-96 rounded-full opacity-30 blur-3xl pointer-events-none"
        style={{ background: 'radial-gradient(circle, rgba(232,163,61,0.25) 0%, transparent 70%)' }}
      />

      <div className="w-full max-w-sm relative">
        <div
          className="rounded-2xl overflow-hidden"
          style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', boxShadow: 'var(--shadow-md)' }}
        >
          {/* 잉크 헤더 */}
          <div className="px-6 pt-7 pb-6 text-center relative overflow-hidden" style={{ background: 'var(--ink)' }}>
            <div
              className="absolute rounded-full"
              style={{ width: 200, height: 200, right: -70, top: -90, background: 'radial-gradient(circle, rgba(232,163,61,0.18) 0%, transparent 70%)' }}
            />
            <div
              className="w-12 h-12 rounded-2xl flex items-center justify-center mx-auto mb-4 relative"
              style={{ background: 'var(--accent-amber)', boxShadow: 'var(--shadow-beacon)' }}
            >
              <span className="beacon-dot beacon-dot--static" style={{ position: 'absolute', top: -3, right: -3, background: 'var(--grade-a)', boxShadow: '0 0 0 2px var(--ink)' }} />
              <span className="font-display font-black text-xl" style={{ color: 'var(--ink)' }}>S</span>
            </div>
            <h1 className="font-display font-black text-2xl relative" style={{ color: 'var(--ink-text)' }}>
              회원가입
            </h1>
            <p className="text-sm mt-1.5 flex items-center justify-center gap-2 relative" style={{ color: 'var(--ink-text-muted)' }}>
              <span className="beacon-dot" />
              현재 위치가 집 위치로 자동 등록됩니다
            </p>
          </div>

          <div className="p-6">
            <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">

              {/* 이메일 + 인증 버튼 */}
              <div>
                <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                  이메일
                </label>
                <div className="flex gap-2">
                  <input
                    type="email"
                    placeholder="email@example.com"
                    className="flex-1 rounded-xl px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-[var(--accent-amber)]"
                    style={inputStyle}
                    disabled={isVerified}
                    {...register('email', { required: '이메일을 입력해주세요.' })}
                  />
                  {!isVerified && (
                    <button
                      type="button"
                      onClick={sendCode}
                      disabled={sending}
                      className="rounded-xl px-4 py-3 text-sm font-semibold shrink-0 disabled:opacity-50 transition-all"
                      style={{ background: 'var(--accent-blue)', color: '#fff' }}
                    >
                      {sending ? '발송 중...' : codeSent ? '재발송' : '인증'}
                    </button>
                  )}
                </div>
                {errors.email && (
                  <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>
                    {errors.email.message}
                  </p>
                )}
              </div>

              {/* 인증코드 입력 */}
              {codeSent && !isVerified && (
                <div>
                  <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                    인증코드
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      placeholder="6자리 코드 입력"
                      maxLength={6}
                      value={verifyCode}
                      onChange={e => setVerifyCode(e.target.value)}
                      className="flex-1 rounded-xl px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-[var(--accent-amber)]"
                      style={inputStyle}
                    />
                    <button
                      type="button"
                      onClick={verifyEmail}
                      disabled={verifying}
                      className="rounded-xl px-4 py-3 text-sm font-semibold shrink-0 disabled:opacity-50 transition-all"
                      style={{ background: 'var(--grade-a)', color: '#fff' }}
                    >
                      {verifying ? '확인 중...' : '확인'}
                    </button>
                  </div>
                </div>
              )}

              {/* 인증 완료 표시 */}
              {isVerified && (
                <div
                  className="rounded-xl px-4 py-2.5 text-sm font-medium flex items-center gap-2"
                  style={{ background: 'var(--grade-a-bg)', color: 'var(--grade-a)' }}
                >
                  <CheckCircle2 size={16} strokeWidth={2} />
                  이메일 인증 완료
                </div>
              )}

              {/* 닉네임 */}
              <div>
                <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                  닉네임
                </label>
                <input
                  placeholder="홍길동"
                  className="w-full rounded-xl px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-[var(--accent-amber)]"
                  style={inputStyle}
                  {...register('nickname', { required: '닉네임을 입력해주세요.' })}
                />
                {errors.nickname && (
                  <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>
                    {errors.nickname.message}
                  </p>
                )}
              </div>

              {/* 비밀번호 */}
              <div>
                <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                  비밀번호
                </label>
                <input
                  type="password"
                  placeholder="8자 이상 입력해주세요"
                  className="w-full rounded-xl px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-[var(--accent-amber)]"
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
                  style={{ background: 'var(--accent-red-soft)', color: 'var(--accent-red)' }}
                >
                  {error}
                </div>
              )}

              <button
                type="submit"
                disabled={isSubmitting || !isVerified}
                className="w-full rounded-xl py-3 text-sm font-display font-bold transition-all disabled:opacity-50"
                style={{ background: 'var(--accent-amber)', color: 'var(--ink)' }}
              >
                {isSubmitting ? '가입 중...' : '가입하기'}
              </button>
            </form>

            <p className="text-center text-sm mt-4" style={{ color: 'var(--text-muted)' }}>
              이미 계정이 있으신가요?{' '}
              <Link to="/login" style={{ color: 'var(--accent-blue)', fontWeight: 600 }}>
                로그인
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
