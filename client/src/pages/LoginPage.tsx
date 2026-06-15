import logo from "@/assets/logo.svg"
import { useAuth } from "@/context/AuthContext"
import { FormEvent, useEffect, useState } from "react"
import { toast } from "react-hot-toast"
import { Link, useNavigate, useLocation } from "react-router-dom"

function LoginPage() {
    const { login, isAuthenticated } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [isSubmitting, setIsSubmitting] = useState(false)

    useEffect(() => {
        if (isAuthenticated) {
            const redirectUrl = location.state?.from || "/dashboard"
            navigate(redirectUrl, { replace: true })
        }
    }, [isAuthenticated, navigate, location.state?.from])

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        if (isSubmitting) return

        try {
            setIsSubmitting(true)
            toast.loading("Signing in...")
            await login(email.trim(), password)
            toast.dismiss()
            toast.success("Welcome back!")
            const redirectUrl = location.state?.from || "/dashboard"
            navigate(redirectUrl)
        } catch (error) {
            toast.dismiss()
            toast.error("Invalid email or password")
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <div className="flex min-h-screen items-center justify-center bg-dark px-4">
            <div className="flex w-full max-w-[500px] flex-col items-center gap-4 p-4 sm:p-8">
                <img src={logo} alt="Logo" className="w-full" />
                <form
                    onSubmit={handleSubmit}
                    className="flex w-full flex-col gap-4"
                >
                    <input
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        className="w-full rounded-md border border-gray-500 bg-darkHover px-3 py-3 focus:outline-none"
                        required
                    />
                    <input
                        type="password"
                        placeholder="Password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        className="w-full rounded-md border border-gray-500 bg-darkHover px-3 py-3 focus:outline-none"
                        required
                    />
                    <button
                        type="submit"
                        disabled={isSubmitting}
                        className="mt-2 w-full rounded-md bg-primary px-8 py-3 text-lg font-semibold text-black disabled:opacity-60"
                    >
                        Login
                    </button>
                </form>
                <p className="text-light">
                    New here?{" "}
                    <Link
                        to="/signup"
                        className="text-primary underline underline-offset-2"
                    >
                        Create an account
                    </Link>
                </p>
            </div>
        </div>
    )
}

export default LoginPage
