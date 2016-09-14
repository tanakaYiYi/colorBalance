import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.util.List;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.TransferHandler;

@SuppressWarnings("serial")
public class CBC extends JFrame{

	public static final int FRAME_WIDTH = 800;
	public static final int BAR_BLOCK = 8;
	
	JTextArea area;				//ファイル名の表示をするテキストエリア
	int bar[];					//各クラスタのピクセル数
	String filepath;			//クラスタリングを行うファイルの絶対パス
	Color color_block[];		//赤、橙、黄、緑、青、紫、白、黒の8色に分類
	
	public static void main(String[] args){
		// TODO Auto-generated method stub
		new CBC();
	}
	
	public CBC(){
		bar = new int[BAR_BLOCK];
		area = new JTextArea();
	    JPanel p = new JPanel();
	    p.add(area);
	    Container contentPane = getContentPane();
	    contentPane.add(p, BorderLayout.CENTER);
		
	    //クラスタの初期値(RGB)とクラスタ数の設定
		color_block = new Color[BAR_BLOCK];		
		color_block[0] = Color.red;
		color_block[1] = Color.orange;
		color_block[2] = Color.yellow;
		color_block[3] = Color.green;
		color_block[4] = Color.blue;
		color_block[5] = Color.magenta;
		color_block[6] = Color.white;
		color_block[7] = Color.black;

		//フレームの設定
		setTitle("Color Balance Checker");		
	    setBounds(100, 100, 800, 100);
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);

		//アプリケーションのフレーム内でドラッグ&ドロップ処理を有効にする
		this.setTransferHandler(new DropFileHandler());
	}	
				
	//クラスタリングの実行
	public void makeColorBar(File file){
		try{
			BufferedImage read = ImageIO.read(file);
			int w = read.getWidth();
			int h = read.getHeight();
			int c,c1;
			int i,j,k;
			double min = 1000;
			int cmin = 0;
			
			//各クラスタの要素数の初期化
			for(i=0;i<bar.length;i++){
				bar[i]=0;
			}
			
			//全ピクセルの探索を行う
			for(i=0;i<w;i++){
				for(j=0;j<h;j++){
					c = read.getRGB(i,j);
					//どの色領域にもっとも近いか計算(クラスタリング)
					for(k=0;k<color_block.length;k++){
						c1 = color_block[k].getRGB();
						if(min>this.calculateDifferenceOnLAB(c,c1)){
							min = this.calculateDifferenceOnLAB(c,c1);
							cmin = k;
						}
					}
					bar[cmin]++;
					min = 1000;		//色差の初期化(値が大きいほど良い)
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}

		repaint();
	}
	
	//クラスタリング結果を描画
	@Override
	public void paint(Graphics g){
		int pixelsum = 0;
		int posx=0;
		int i;
		
		//画像の総ピクセル数を格納
		for(i=0;i<bar.length;i++){
			pixelsum += bar[i];
		}
		
		//総ピクセル数が0でないならば（読み込むための画像が存在するならば）、カラーバーの描画
		if(pixelsum != 0){
			for(i=0;i<color_block.length;i++){
				g.setColor(color_block[i]);
				g.fillRect(posx, 50, FRAME_WIDTH*bar[i]/pixelsum, 50);
				posx += FRAME_WIDTH*bar[i]/pixelsum;
			}
		}
		area.setText(filepath);
	}
	
	//ドロップ処理
	public class DropFileHandler extends TransferHandler {		
		
		@Override
		public boolean canImport(TransferSupport support){
			//ドロップ操作でない場合受け取らない
			if(!support.isDrop()) {
				return false;
			}
			//ファイルでない場合受け取らない
			if(!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
				return false;
			}
			return true;
		}
		
		//ドロップされたファイル名を1つだけ取得し、makeColorBarメソッドを呼び出す
		@SuppressWarnings("unchecked")
		@Override
		public boolean importData(TransferSupport support){
			if(!canImport(support)){
				return false;
			}
			
			Transferable t = support.getTransferable();
			try{
				//ドロップされたファイルをリストで取得
				List<File> files = (List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);
				
				if(files.size() > 1){
					//エラーメッセージ
					errorMsg("同時にドロップできるファイルは1つだけです。");
				}
				else if(files.size() == 1){
					File file = files.get(0);
					filepath = new String();
					filepath = file.getAbsolutePath();
					File imagefile = new File(filepath);
					makeColorBar(imagefile);
				}				
			}
			catch(UnsupportedFlavorException | IOException e){
				e.printStackTrace();
			}
			return true;
		}
	}
	
	//ダイアログメッセージ表示
	public void errorMsg(String msg){
		JOptionPane.showMessageDialog(null, msg);
	}
	
	//2つのRGB値から色差を計算（色差はCIELAB2000色差式に基づく）
	public double calculateDifferenceOnLAB(int color1, int color2){
		double r1 = (color1>>16&0xff);
		double g1 = (color1>>8&0xff);
		double b1 = (color1&0xff);
		double r2 = (color2>>16&0xff);
		double g2 = (color2>>8&0xff);
		double b2 = (color2&0xff);
		double ls1, as1, bs1, ls2, as2, bs2;
		double difference;
		
		double x1 = 100*(0.3933*Math.pow((r1/255),2.2)
				+0.3651*Math.pow((g1/255),2.2)
				+0.1903*Math.pow((b1/255),2.2));
		double y1 = 100*(0.2123*Math.pow((r1/255),2.2)
				+0.7010*Math.pow((g1/255),2.2)
				+0.0858*Math.pow((b1/255),2.2));
		double z1 = 100*(0.0182*Math.pow((r1/255),2.2)
				+0.1117*Math.pow((g1/255),2.2)
				+0.9570*Math.pow((b1/255),2.2));
		double x2 = 100*(0.3933*Math.pow((r2/255),2.2)
				+0.3651*Math.pow((g2/255),2.2)
				+0.1903*Math.pow((b2/255),2.2));
		double y2 = 100*(0.2123*Math.pow((r2/255),2.2)
				+0.7010*Math.pow((g2/255),2.2)
				+0.0858*Math.pow((b2/255),2.2));
		double z2 = 100*(0.0182*Math.pow((r2/255),2.2)
				+0.1117*Math.pow((g2/255),2.2)
				+0.9570*Math.pow((b2/255),2.2));
		double xn = 98.071;
		double yn = 100;
		double zn = 118.226;
		
		if(y1/yn > 0.008856) ls1 = 116*Math.pow((y1/yn),(double)1/3)-16;
		else ls1=903.29*(y1/yn);
		as1 = 500*(Math.pow(x1/xn,1/3) - Math.pow(y1/yn,(double)1/3));
		bs1 = 200*(Math.pow(y1/yn,1/3) - Math.pow(z1/zn,(double)1/3));

		if(y2/yn > 0.008856) ls2 = 116*Math.pow((y2/yn),(double)1/3)-16;
		else ls2=903.29*(y2/yn);
		as2 = 500*(Math.pow(x2/xn,1/3) - Math.pow(y2/yn,(double)1/3));
		bs2 = 200*(Math.pow(y2/yn,1/3) - Math.pow(z2/zn,(double)1/3));
		
		difference=Math.pow(
				Math.pow((ls1-ls2),2)+Math.pow((as1-as2),2)+Math.pow((bs1-bs2),2),(double)1/2);
				
		return difference;
	}
	
}
