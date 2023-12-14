package activesupport.mail.enums;


import jakarta.mail.Folder;

public enum FolderPermission {

    ReadOnly(Folder.READ_ONLY),
    ReadWrite(Folder.READ_WRITE);

    private int rights;

    FolderPermission(int rights) {
        this.rights = rights;
    }

    public int getPermission(){
        return rights;
    }

}
