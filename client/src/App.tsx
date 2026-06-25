import { Route, BrowserRouter as Router, Routes } from "react-router-dom"
import ProtectedRoute from "./components/auth/ProtectedRoute"
import Toast from "./components/toast/Toast"
import DashboardPage from "./pages/DashboardPage"
import EditorPage from "./pages/EditorPage"
import HomePage from "./pages/HomePage"
import LoginPage from "./pages/LoginPage"
import SignupPage from "./pages/SignupPage"

const App = () => {
    return (
        <>
            <Router>
                <Routes>
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/signup" element={<SignupPage />} />
                    <Route
                        path="/dashboard"
                        element={
                            <ProtectedRoute>
                                <DashboardPage />
                            </ProtectedRoute>
                        }
                    />
                    <Route path="/" element={<HomePage />} />
                    <Route
                        path="/editor/:roomId"
                        element={
                            <ProtectedRoute>
                                <EditorPage />
                            </ProtectedRoute>
                        }
                    />
                </Routes>
            </Router>
            <Toast />
        </>
    )
}

export default App
