public class BackupService{
 public void backup(String path)throws Exception{
  Runtime.getRuntime().exec("tar -czf out.tar "+path);
 }
}