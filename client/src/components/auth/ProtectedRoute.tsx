import { Navigate, useLocation } from "react-router-dom"
import { useAuth } from "@/context/AuthContext"
import { ReactNode } from "react"

function ProtectedRoute({ children }: { children: ReactNode }) {
    const { isAuthenticated, isLoading } = useAuth()
    const location = useLocation()

    if (isLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-dark text-light">
                Loading...
            </div>
        )
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />
    }

    return children
}

export default ProtectedRoute
