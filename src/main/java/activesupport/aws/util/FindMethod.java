package activesupport.aws.util;

import activesupport.aws.s3.S3;

import java.lang.reflect.Method;

public class FindMethod {

     public static String name(){
         String methodName = "";
         try{
             Class<S3> classObject = S3.class;

             var methods = classObject.getMethods();
             for(Method method : methods){
                 methodName += method.getName();
             }
         }catch (Exception e){
             e.printStackTrace();
         }
         return methodName;
     }
}
