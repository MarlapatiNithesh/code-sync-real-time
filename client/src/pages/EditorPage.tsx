import SplitterComponent from "@/components/SplitterComponent"
import ConnectionStatusPage from "@/components/connection/ConnectionStatusPage"
import Sidebar from "@/components/sidebar/Sidebar"
import WorkSpace from "@/components/workspace"
import { useAppContext } from "@/context/AppContext"
import { useSocket } from "@/context/SocketContext"
import useFullScreen from "@/hooks/useFullScreen"
import useUserActivity from "@/hooks/useUserActivity"
import { SocketEvent } from "@/types/socket"
import { USER_STATUS, User } from "@/types/user"
import { useAuth } from "@/context/AuthContext"
import { useEffect } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { getRoomByCode, Room } from "@/api/authApi"

function EditorPage() {
    // Listen user online/offline status
    useUserActivity()
    // Enable fullscreen mode
    useFullScreen()
    const navigate = useNavigate()
    const { roomId } = useParams()
    const { status, setCurrentUser, currentUser } = useAppContext()
    const { socket } = useSocket()
    const { user: authUser } = useAuth()

    useEffect(() => {
        if (currentUser.username.length > 0) return
        const username = authUser?.username
        if (username === undefined) {
            navigate("/login", {
                state: { from: `/editor/${roomId}` },
            })
        } else if (roomId) {
            const user: User = { username, roomId }
            setCurrentUser(user)
            socket.emit(SocketEvent.JOIN_REQUEST, user)
        }
    }, [
        currentUser.username,
        authUser?.username,
        navigate,
        roomId,
        setCurrentUser,
        socket,
    ])

    useEffect(() => {
        if (!roomId || !authUser?.email) return
        const saveRoomToJoinedList = async () => {
            try {
                const roomInfo = await getRoomByCode(roomId)
                const storageKey = `joinedRooms_${authUser.email}`
                const storedJoinedRooms = localStorage.getItem(storageKey)
                let joinedRoomsList: Room[] = storedJoinedRooms ? JSON.parse(storedJoinedRooms) : []
                
                // Add if not already present
                if (!joinedRoomsList.some(r => r.roomCode === roomId)) {
                    // Only list if the user is not the owner (since they see those under Created Rooms)
                    if (roomInfo.ownerId !== authUser.id) {
                        joinedRoomsList = [roomInfo, ...joinedRoomsList]
                        localStorage.setItem(storageKey, JSON.stringify(joinedRoomsList))
                    }
                }
            } catch (err) {
                console.error("Failed to fetch room details for storage", err)
            }
        }
        saveRoomToJoinedList()
    }, [roomId, authUser])

    if (status === USER_STATUS.CONNECTION_FAILED) {
        return <ConnectionStatusPage />
    }

    return (
        <SplitterComponent>
            <Sidebar />
            <WorkSpace/>
        </SplitterComponent>
    )
}

export default EditorPage
