import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class CreateDB 
{
	static int currentversion;
	static StringBuffer InsertQuery1 = new StringBuffer("insert into PathDB(version, Source, Region, ASName, IsSent");
	static StringBuffer InsertQuery3;
	static StringBuffer InsertQuery5 = new StringBuffer(")");
	static int MaxHops = 24;
	static String[] RegionList = {"Mumbai", "Delhi+(Noida)","Kolkata", "Hyderabad"};
	
	public static void main(String args[]) 
	{
		System.out.println("started main from NIXIpdate");
		try {
			setUpDatabase();
		} catch (ClassNotFoundException e) {
			
			e.printStackTrace();
		}
		for(String Region: RegionList)
		{
			try {
				GetAllPaths(Region);
			} catch (HttpException e) {
				
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				
				e.printStackTrace();
			} catch (IOException e) {
				
				e.printStackTrace();
			} catch (SQLException e) {
				
				e.printStackTrace();
			}
		}
	}
	 static void setUpDatabase() throws ClassNotFoundException
	{
		

		try {
			Class.forName("org.h2.Driver");
			Connection conn = DriverManager.getConnection("jdbc:h2:C:/Users/Anshuli-PC/workspace/ProjNetworking/NIXIProj", "sa", "");
			Statement stat = conn.createStatement();

			try
			{
				
				stat.execute("create table PathDB(version int, id int primary key auto_increment, Source varchar(255), Region varchar(255), ASName varchar(255), IsSent Boolean, Network varchar(255), Hops int," +
					"Hop1 varchar(255), Hop2 varchar(255), Hop3 varchar(255), Hop4 varchar(255), Hop5 varchar(255), " +
					"Hop6 varchar(255), Hop7 varchar(255), Hop8 varchar(255), Hop9 varchar(255), Hop10 varchar(255), " +
					"Hop11 varchar(255), Hop12 varchar(255), Hop13 varchar(255), Hop14 varchar(255), Hop15 varchar(255), " +
					"Hop16 varchar(255), Hop17 varchar(255), Hop18 varchar(255), Hop19 varchar(255), Hop20 varchar(255), )");
				System.out.println("Database table created");
			}
			catch(Exception e)
			{
				System.out.println(e);
			}
			
			currentversion=0;
			try
			{
				stat.execute("create table versiondb(time timestamp default CURRENT_TIMESTAMP, version int)");
				currentversion=1;
			}
			catch(Exception e)
			{
				ResultSet rs = stat.executeQuery("select max(version) from versiondb");
				while(rs.next())
				{
					currentversion=Integer.parseInt(rs.getString("max(version)"));
				}
				currentversion++;
			}
			stat.execute("insert into versiondb(version) values("+ Integer.toString(currentversion) + ")");
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	static String GetASNum(String IP, String Region) throws HttpException, IOException
	{
		HttpClient client = new HttpClient();
	    String url = "http://203.190.131.164/lg/?query=bgp&protocol=IPv4&addr=neighbors+"+IP+"&router=NIXI+"+Region;
	    
	    // Create a method instance.
	    GetMethod method = new GetMethod(url);
	    

	      // Execute the method.
	      int statusCode = client.executeMethod(method);

	      if (statusCode != HttpStatus.SC_OK) 
	      {
	        System.err.println("Method failed: " + method.getStatusLine());
	      }

	      InputStream Body = method.getResponseBodyAsStream();	      
	      Document doc = Jsoup.parse(Body, null, url);
	      
	      Element link = doc.getElementsByTag("CODE").first().getElementsByTag("A").get(1);
	      method.releaseConnection();
	      return link.text().trim();
  
	}
	static String GetISPName(String IP, String Region) throws ClassNotFoundException, SQLException, HttpException, IOException
	{
		String ASNum = GetASNum(IP, Region);
		String ISPName = null;
		
		Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("jdbc:h2:C:/Users/Anshuli-PC/workspace/ProjNetworking/NIXIProj", "sa", "");
        Statement stat = conn.createStatement();
        
        ResultSet rs;
        rs = stat.executeQuery("select * from ispnames where ASnum = '"+ASNum+"'");
        while(rs.next())
        {
        	ISPName = rs.getString("shortname");
        }
        
        return ISPName;
	}
	static void GetRoutes(String IP, Boolean isSent, String ISPName, String Region) throws HttpException, IOException, ClassNotFoundException, SQLException
	{
 		InsertQuery3 = new StringBuffer(",Hops, Network) values(" +Integer.toString(currentversion) +", 'NIXI','");
		StringBuffer InsertQuery2 = new StringBuffer();
		StringBuffer InsertQuery4 = new StringBuffer();
		StringBuffer InsertQuery = new StringBuffer();
		
		InsertQuery4.append(Region+"','");
		InsertQuery4.append(ISPName+"',");
		InsertQuery4.append(isSent.toString());

		HttpClient client = new HttpClient();
		String url = "http://203.190.131.164/lg/?query=bgp&protocol=IPv4&addr=neighbors+" + IP + "+";
		if(isSent)
		{
			url+="advertised-";
		}
		url += "routes&router=NIXI+"+Region;
		
		GetMethod method = new GetMethod(url);
			Class.forName("org.h2.Driver");
	        Connection conn = DriverManager.getConnection("jdbc:h2:C:/Users/Anshuli-PC/workspace/ProjNetworking/NIXIProj", "sa", "");
	        Statement stat = conn.createStatement();
			
		
	      // Execute the method.
	      client.executeMethod(method);

	      //Parse Response
	      InputStream Body = method.getResponseBodyAsStream();	      
	      Document doc = Jsoup.parse(Body, null, url);
	      Element codeElement = doc.getElementsByTag("CODE").first();
	      Elements links = codeElement.getElementsByTag("A");
	  
	      //Form query
	      int hops=0;
	      StringBuffer Network = new StringBuffer();
	      for (Element link : links) 
	      {
	    	  try
	    	  {
	    		  Integer.parseInt(link.text());
	    		  InsertQuery4.append("," + link.text());
	    		  hops++;
	    	  }
	    	  catch(NumberFormatException e)
	    	  {
	    	      InsertQuery4.append("," + Integer.toString(hops)+",'"+Network+"'");
	    	      
	    	      	for(int i=1; i<=hops; i++)
	    	      	{
	    	      		InsertQuery2.append(",Hop"+Integer.toString(i));
	    	      	}

	    	      	InsertQuery.append(InsertQuery1);
	    	      	InsertQuery.append(InsertQuery2);
	    	      	InsertQuery.append(InsertQuery3);
	    	      	InsertQuery.append(InsertQuery4);
	    	      	InsertQuery.append(InsertQuery5);
	    	    
	    	      	
	    	      	if(hops>MaxHops)
	    	      	{
	    	      		while(hops>MaxHops)
	    	      		{
	    	      			MaxHops++;
	    	      			stat.execute("alter Table PathDB add column Hop"+Integer.toString(MaxHops)+" varchar(255)");
	    	      			System.out.println("inserted into table");
	    	      		}
	    	      	}
	    	      	
	    	      	if(hops>0)
	    	      		stat.execute(InsertQuery.toString());
	    	      	
	    	      	InsertQuery = new StringBuffer();
	    	      	InsertQuery2 = new StringBuffer();
	    	      	InsertQuery4 = new StringBuffer();
	    			InsertQuery4.append(Region+"','");
	    	      	InsertQuery4.append(ISPName+"',");
	    			InsertQuery4.append(isSent.toString());
	    	      	hops=0;
	    	      	Network = new StringBuffer(link.text());
	    	      	
	    	  }
	      }
  
	}
	
	
	@SuppressWarnings("deprecation")
	static void GetAllPaths(String Region) throws HttpException, IOException, ClassNotFoundException, SQLException
	{	
	    HttpClient client = new HttpClient();
	    String url = "http://203.190.131.164/lg/";
	    
	    // Create a method instance.
	   PostMethod method = new PostMethod(url);
	   
	   //--deprecated
	   method.setRequestBody("query=summary&protocol=IPv4&addr=&router=NIXI+"+Region);	    
	   /* NameValuePair[] params = null  ;
	    params[0]=new NameValuePair("query","summary");
	    params[1]=new NameValuePair("protocol","IPv4");
	    params[2]=new NameValuePair("addr","");
	    params[3]=new NameValuePair("router","NIXI"+Region);
	    method.setRequestBody(params);*/
	    
	      // Execute the method.
	      client.executeMethod(method);
	      System.out.println("received response from NIXI looking glass");
	      //Parse the response
	      InputStream Body = method.getResponseBodyAsStream();	      
	      Document doc = Jsoup.parse(Body, null, url);      
	      Element codeElement = doc.getElementsByTag("CODE").first();
	      Elements links = codeElement.getElementsByTag("B");
	      
	      
	      //Process the Response
	      for (Element link : links) 
	      {
	    	  try
	    	  {
	    		  Integer.parseInt(link.text());
	    	  }
	    	  catch(NumberFormatException e)
	    	  {
	    		  String S=GetISPName(link.text().trim(), Region);
	    		  GetRoutes(link.text(), false, S, Region);
	    		  GetRoutes(link.text(), true, S, Region);
	    	  }
	      }

	      // Release the connection.
	      method.releaseConnection();
	    
	    
	}
	
}
