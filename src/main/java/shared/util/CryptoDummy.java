package shared.util;

import java.io.File;  
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
public   class CryptoDummy
{  private     byte[]   textoCifrado;
   private     byte[]   textoDecifrado;
   public   CryptoDummy()
   {  textoCifrado = null;
      textoDecifrado = null;
   }
   public void geraChave(File fDummy) throws IOException  
   {  // Gera uma chave Dummy simetrica (dk: 0 a 100):
      int   dk = (int) (Math.random()*101);
      // Grava a chave Dummy simetrica em formato serializado  
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fDummy));  
      oos.writeObject(dk);  
      oos.close();
   }
   public  void  geraCifra(byte[] texto, File fDummy)
   	throws   IOException, ClassNotFoundException
   {  ObjectInputStream ois = new ObjectInputStream (new FileInputStream (fDummy));  
      int iDummy = (Integer) ois.readObject();
      ois.close();
      textoCifrado = texto;
      for(int i = 0; i < texto.length; i++)
      {  textoCifrado[i] = (byte) (textoCifrado[i] + i + iDummy);
      }
   }
   public  byte[]   getTextoCifrado() throws   Exception
   {  return   textoCifrado;
   }
   public  void  geraDecifra(byte[] texto, File fDummy)
   	throws   IOException, ClassNotFoundException
   {  ObjectInputStream ois = new ObjectInputStream (new FileInputStream (fDummy));  
      int iDummy = (Integer) ois.readObject();
      ois.close();
      textoDecifrado = texto;
      for(int i = 0; i < texto.length; i++)
      {  textoDecifrado[i] = (byte) (textoDecifrado[i] - i - iDummy);
      }
   }
   public  byte[]   getTextoDecifrado()  throws   Exception
   {  return   textoDecifrado;
   }
}

