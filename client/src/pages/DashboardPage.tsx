import logo from "@/assets/logo.svg"
import { createRoom, getMyRooms, getRoomByCode, Room } from "@/api/authApi"
import { useAuth } from "@/context/AuthContext"
import { FormEvent, useEffect, useState, useRef } from "react"
import { toast } from "react-hot-toast"
import { useNavigate } from "react-router-dom"

function DashboardPage() {
    const { user, logout } = useAuth()
    const navigate = useNavigate()
    
    // Created Rooms (Paginated from Backend)
    const [rooms, setRooms] = useState<Room[]>([])
    const [createdPage, setCreatedPage] = useState(0)
    const [hasMoreCreated, setHasMoreCreated] = useState(true)
    const [isLoading, setIsLoading] = useState(true)

    // Joined Rooms (Paginated locally from localStorage)
    const [allJoinedRooms, setAllJoinedRooms] = useState<Room[]>([])
    const [visibleJoinedRooms, setVisibleJoinedRooms] = useState<Room[]>([])
    const [joinedPage, setJoinedPage] = useState(0)

    const [roomName, setRoomName] = useState("")
    const [joinRoomId, setJoinRoomId] = useState("")

    const PAGE_SIZE = 5

    // Refs for scroll pagination detection (Intersection Observer)
    const createdObserverRef = useRef<HTMLDivElement | null>(null)
    const joinedObserverRef = useRef<HTMLDivElement | null>(null)

    const loadRooms = async (page: number, append = false) => {
        try {
            const data = await getMyRooms(page, PAGE_SIZE)
            if (append) {
                setRooms((prev) => [...prev, ...data])
            } else {
                setRooms(data)
            }
            if (data.length < PAGE_SIZE) {
                setHasMoreCreated(false)
            } else {
                setHasMoreCreated(true)
            }
        } catch {
            toast.error("Failed to load your rooms")
        } finally {
            setIsLoading(false)
        }
    }

    const loadJoinedRooms = () => {
        if (user?.email) {
            const stored = localStorage.getItem(`joinedRooms_${user.email}`)
            if (stored) {
                const parsed: Room[] = JSON.parse(stored)
                setAllJoinedRooms(parsed)
                setVisibleJoinedRooms(parsed.slice(0, PAGE_SIZE))
                setJoinedPage(0)
            }
        }
    }

    // Initial load when user state changes
    useEffect(() => {
        setCreatedPage(0)
        loadRooms(0, false)
        loadJoinedRooms()
    }, [user])

    // Infinite Scroll Observer for Created Rooms
    useEffect(() => {
        if (!hasMoreCreated || isLoading) return

        const observer = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting) {
                    const nextPage = createdPage + 1
                    setCreatedPage(nextPage)
                    loadRooms(nextPage, true)
                }
            },
            { threshold: 0.1 }
        )

        const currentRef = createdObserverRef.current
        if (currentRef) {
            observer.observe(currentRef)
        }

        return () => {
            if (currentRef) {
                observer.unobserve(currentRef)
            }
        }
    }, [createdPage, hasMoreCreated, isLoading])

    // Infinite Scroll Observer for Joined Rooms
    useEffect(() => {
        if (visibleJoinedRooms.length >= allJoinedRooms.length) return

        const observer = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting) {
                    const nextPage = joinedPage + 1
                    setJoinedPage(nextPage)
                    setVisibleJoinedRooms(allJoinedRooms.slice(0, (nextPage + 1) * PAGE_SIZE))
                }
            },
            { threshold: 0.1 }
        )

        const currentRef = joinedObserverRef.current
        if (currentRef) {
            observer.observe(currentRef)
        }

        return () => {
            if (currentRef) {
                observer.unobserve(currentRef)
            }
        }
    }, [joinedPage, visibleJoinedRooms.length, allJoinedRooms.length])

    const handleCreateRoom = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        if (roomName.trim().length < 3) {
            toast.error("Room name must be at least 3 characters")
            return
        }

        try {
            toast.loading("Creating room...")
            const room = await createRoom(roomName.trim())
            toast.dismiss()
            toast.success("Room created")
            setRoomName("")
            setRooms((prev) => [room, ...prev])
        } catch {
            toast.dismiss()
            toast.error("Failed to create room")
        }
    }

    const handleJoinRoom = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        const code = joinRoomId.trim()
        if (code.length < 5) {
            toast.error("Room ID must be at least 5 characters")
            return
        }

        try {
            toast.loading("Finding room...")
            const room = await getRoomByCode(code)
            toast.dismiss()
            toast.success("Room found! Joining...")

            if (user?.email && room.ownerId !== user.id) {
                const storageKey = `joinedRooms_${user.email}`
                const storedJoinedRooms = localStorage.getItem(storageKey)
                let list: Room[] = storedJoinedRooms ? JSON.parse(storedJoinedRooms) : []
                if (!list.some(r => r.roomCode === code)) {
                    list = [room, ...list]
                    localStorage.setItem(storageKey, JSON.stringify(list))
                }
            }
            navigate(`/editor/${code}`)
        } catch (error) {
            toast.dismiss()
            toast.error("Room not found or invalid Room ID")
        }
    }

    const openRoom = (roomCode: string) => {
        navigate(`/editor/${roomCode}`)
    }

    return (
        <div className="min-h-screen bg-dark px-4 py-10 text-light">
            <div className="mx-auto flex w-full max-w-4xl flex-col gap-8">
                <div className="flex flex-col items-center gap-4 sm:flex-row sm:justify-between">
                    <img src={logo} alt="Logo" className="w-full max-w-[280px]" />
                    <div className="flex items-center gap-4">
                        <span>Hi, {user?.username}</span>
                        <button
                            onClick={logout}
                            className="rounded-md border border-gray-500 px-4 py-2 hover:bg-darkHover transition-colors"
                        >
                            Logout
                        </button>
                    </div>
                </div>

                <div className="rounded-md border border-darkHover bg-[#2A2D34] p-6">
                    <h1 className="mb-6 text-2xl font-semibold text-primary">
                        Room Hub
                    </h1>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                        {/* Create Room Box */}
                        <div className="rounded-lg bg-darkHover p-5 border border-gray-700">
                            <h3 className="text-lg font-medium mb-3 text-[#50fa7b]">Create a New Room</h3>
                            <form onSubmit={handleCreateRoom} className="flex flex-col gap-3">
                                <input
                                    type="text"
                                    placeholder="Enter room name"
                                    value={roomName}
                                    onChange={(event) => setRoomName(event.target.value)}
                                    className="w-full rounded-md border border-gray-600 bg-dark px-3 py-2.5 focus:outline-none focus:border-primary text-sm text-light"
                                    required
                                />
                                <button
                                    type="submit"
                                    className="rounded-md bg-primary py-2.5 font-semibold text-black hover:opacity-90 transition-opacity text-sm"
                                >
                                    Create Room
                                </button>
                            </form>
                        </div>

                        {/* Join Room Box */}
                        <div className="rounded-lg bg-darkHover p-5 border border-gray-700">
                            <h3 className="text-lg font-medium mb-3 text-[#50fa7b]">Join Room by ID</h3>
                            <form onSubmit={handleJoinRoom} className="flex flex-col gap-3">
                                <input
                                    type="text"
                                    placeholder="Enter Room ID"
                                    value={joinRoomId}
                                    onChange={(event) => setJoinRoomId(event.target.value)}
                                    className="w-full rounded-md border border-gray-600 bg-dark px-3 py-2.5 focus:outline-none focus:border-primary text-sm text-light"
                                    required
                                />
                                <button
                                    type="submit"
                                    className="rounded-md bg-primary py-2.5 font-semibold text-black hover:opacity-90 transition-opacity text-sm"
                                >
                                    Join Room
                                </button>
                            </form>
                        </div>
                    </div>

                    {isLoading ? (
                        <p>Loading rooms...</p>
                    ) : (
                        <div className="flex flex-col gap-8">
                            {/* Created Rooms Section */}
                            <div>
                                <h2 className="text-xl font-semibold mb-3 text-[#50fa7b]">Rooms Created by You</h2>
                                {rooms.length === 0 ? (
                                    <p className="text-gray-400 text-sm">No rooms created yet. Create one above.</p>
                                ) : (
                                    <div className="flex flex-col gap-3 max-h-[350px] overflow-y-auto pr-1">
                                        {rooms.map((room) => (
                                            <div
                                                key={room.id}
                                                className="flex flex-col gap-3 rounded-md border border-gray-600 bg-darkHover p-4 sm:flex-row sm:items-center sm:justify-between"
                                            >
                                                <div>
                                                    <h2 className="text-lg font-semibold">{room.name}</h2>
                                                    <p className="text-sm text-gray-400">
                                                        Room ID: <span className="font-mono">{room.roomCode}</span>
                                                    </p>
                                                </div>
                                                <button
                                                    onClick={() => openRoom(room.roomCode)}
                                                    className="rounded-md bg-primary px-5 py-2 font-semibold text-black hover:opacity-90 transition-opacity"
                                                >
                                                    Open & Join
                                                </button>
                                            </div>
                                        ))}
                                        {hasMoreCreated && (
                                            <div ref={createdObserverRef} className="py-4 text-center text-gray-500 text-sm">
                                                Loading more rooms...
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>

                            {/* Joined Rooms Section */}
                            <div>
                                <h2 className="text-xl font-semibold mb-3 text-[#50fa7b]">Rooms You've Joined</h2>
                                {visibleJoinedRooms.length === 0 ? (
                                    <p className="text-gray-400 text-sm">No joined rooms yet. Enter a Room ID above to join one.</p>
                                ) : (
                                    <div className="flex flex-col gap-3 max-h-[350px] overflow-y-auto pr-1">
                                        {visibleJoinedRooms.map((room) => (
                                            <div
                                                key={room.id}
                                                className="flex flex-col gap-3 rounded-md border border-gray-600 bg-darkHover p-4 sm:flex-row sm:items-center sm:justify-between"
                                            >
                                                <div>
                                                    <h2 className="text-lg font-semibold">{room.name}</h2>
                                                    <p className="text-sm text-gray-400">
                                                        Room ID: <span className="font-mono">{room.roomCode}</span>
                                                        <span className="ml-2 text-xs text-gray-500">(Owner: {room.ownerUsername})</span>
                                                    </p>
                                                </div>
                                                <button
                                                    onClick={() => openRoom(room.roomCode)}
                                                    className="rounded-md bg-primary px-5 py-2 font-semibold text-black hover:opacity-90 transition-opacity"
                                                >
                                                    Open & Join
                                                </button>
                                            </div>
                                        ))}
                                        {visibleJoinedRooms.length < allJoinedRooms.length && (
                                            <div ref={joinedObserverRef} className="py-4 text-center text-gray-500 text-sm">
                                                Loading more joined rooms...
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

export default DashboardPage
