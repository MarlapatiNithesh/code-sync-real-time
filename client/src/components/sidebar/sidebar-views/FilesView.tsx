import FileStructureView from "@/components/files/FileStructureView"
import useResponsive from "@/hooks/useResponsive"

function FilesView() {
    const { viewHeight } = useResponsive()

    return (
        <div
            className="flex select-none flex-col gap-1 px-4 py-2"
            style={{ height: viewHeight, maxHeight: viewHeight }}
        >
            <FileStructureView />
        </div>
    )
}

export default FilesView
