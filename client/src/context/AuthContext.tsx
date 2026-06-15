import {
    AuthResponse,
    UserProfile,
    getProfile,
    login as loginRequest,
    signup as signupRequest,
} from "@/api/authApi"
import {
    ReactNode,
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
} from "react"

interface AuthContextType {
    token: string | null
    user: UserProfile | null
    isAuthenticated: boolean
    isLoading: boolean
    login: (email: string, password: string) => Promise<AuthResponse>
    signup: (
        email: string,
        username: string,
        password: string,
    ) => Promise<AuthResponse>
    logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export const useAuth = () => {
    const context = useContext(AuthContext)
    if (!context) {
        throw new Error("useAuth must be used within AuthProvider")
    }
    return context
}

const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [token, setToken] = useState<string | null>(
        localStorage.getItem("authToken"),
    )
    const [user, setUser] = useState<UserProfile | null>(null)
    const [isLoading, setIsLoading] = useState(true)

    const persistAuth = useCallback((auth: AuthResponse) => {
        localStorage.setItem("authToken", auth.token)
        localStorage.setItem("authUser", JSON.stringify(auth))
        setToken(auth.token)
        setUser({
            id: auth.userId,
            email: auth.email,
            username: auth.username,
        })
    }, [])

    const logout = useCallback(() => {
        localStorage.removeItem("authToken")
        localStorage.removeItem("authUser")
        setToken(null)
        setUser(null)
    }, [])

    const login = useCallback(
        async (email: string, password: string) => {
            const auth = await loginRequest({ email, password })
            persistAuth(auth)
            return auth
        },
        [persistAuth],
    )

    const signup = useCallback(
        async (email: string, username: string, password: string) => {
            const auth = await signupRequest({ email, username, password })
            persistAuth(auth)
            return auth
        },
        [persistAuth],
    )

    useEffect(() => {
        const bootstrap = async () => {
            if (!token) {
                setIsLoading(false)
                return
            }

            try {
                const profile = await getProfile()
                setUser(profile)
            } catch {
                logout()
            } finally {
                setIsLoading(false)
            }
        }

        bootstrap()
    }, [logout, token])

    const value = useMemo(
        () => ({
            token,
            user,
            isAuthenticated: Boolean(token && user),
            isLoading,
            login,
            signup,
            logout,
        }),
        [isLoading, login, logout, signup, token, user],
    )

    return (
        <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
    )
}

export { AuthProvider }
export default AuthContext
