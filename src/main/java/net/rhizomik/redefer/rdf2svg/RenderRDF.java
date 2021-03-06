package net.rhizomik.redefer.rdf2svg;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.rhizomik.jena.builtin.StrContains;
import net.rhizomik.jena.builtin.StrNotContains;
import net.rhizomik.redefer.util.DefaultNSPrefixes;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.FileUtils;

/**
 * @author: http://rhizomik.net/~roberto
 *
 */
public class RenderRDF extends HttpServlet 
{
	static String defaultLineLength = "50";
	static String defaultMaxLineLength = "80";
	private String MAX_INPUT_SIZE = "25000";

	private String dotPath;

    private String rdf;
    private String rules;
    private String format;
    private String mode;
	private String language;
	private String namespaces;
    private int maxSize;
    private int lineLength;
    private int maxLineLength;

    public RenderRDF()
	{
		super();
		BuiltinRegistry.theRegistry.register(new StrContains());
		BuiltinRegistry.theRegistry.register(new StrNotContains());
	}	

	public void init(ServletConfig config) throws ServletException 
	{
		super.init(config);
        dotPath = config.getInitParameter("dotPath")!=null?
        		  config.getInitParameter("dotPath"):
        		  "C:\\Programs\\GraphViz\\bin\\dot.exe";
        if (config.getInitParameter("maxlength") != null)
        	MAX_INPUT_SIZE = config.getInitParameter("maxlength");
        //System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
	}

	public void destroy() {
		super.destroy();
	}

    public void doGet(HttpServletRequest request, HttpServletResponse response)
    	throws javax.servlet.ServletException, java.io.IOException {
        performTask(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws javax.servlet.ServletException, java.io.IOException {
        performTask(request, response);
    }

	public void performTask(HttpServletRequest request, HttpServletResponse response) 
		throws javax.servlet.ServletException, IOException
	{
		request.setCharacterEncoding("UTF-8");
        String appBase = request.getRequestURL().
                substring(0, request.getRequestURL().lastIndexOf("/"));

        rdf = getInput(request, "rdf");
		rules = getInput(request, "rules", appBase+"/rules/showgraph.jrule");
		format = getInput(request, "format");
		mode = getInput(request, "mode", "svg");
		
		// Parameters for the XSLT transformation from RDF to DOT
		language = getInput(request, "language");
		namespaces = getInput(request, "namespaces");
		
		// Set maximum input size
        maxSize  = Integer.parseInt(getInput(request, "maxsize", MAX_INPUT_SIZE));

		// Define default line length for text nodes and also the maximum length
        lineLength  = Integer.parseInt(getInput(request, "length", defaultLineLength));
        maxLineLength  = Integer.parseInt(getInput(request, "maxlength", defaultMaxLineLength));
        if (maxLineLength < lineLength)
					maxLineLength = lineLength;

		Model data = ModelFactory.createDefaultModel();
		data.setNsPrefixes(DefaultNSPrefixes.map);
        try
        {            
            URL rdfURL = new URL(rdf);
            if (format == null)
            	format = FileUtils.guessLang(rdf);
            HttpURLConnection urlConn = (HttpURLConnection)rdfURL.openConnection();
            urlConn.setRequestProperty("Accept", "application/rdf+xml, text/plain;q=0.5, text/rdf+n3;q=0.6");
            if (urlConn.getContentLength() > maxSize)
            	throw new ServletException("Sorry, this service is not able to transform inputs bigger than "+maxSize+" bytes");
            data.read(urlConn.getInputStream(), rdf, format);            
        } 
        catch (MalformedURLException e)
        {
            // If rdf is not an URL, consider it is directly the RDF to render
        	data.read(new StringReader(rdf), "", format);
        }
        
		try
		{
			Model graph = runInference(data, new URL(rules), lineLength, maxLineLength);
			
			String base = this.getServletContext().getRealPath("/");
			Source xslSource = new StreamSource(new File(base+"/xsl/rdf2dot.xsl"));
			ByteArrayOutputStream dot = rdfModel2dot(xslSource, graph);
						
			Runtime rt = Runtime.getRuntime();
	        Process pr = rt.exec(dotPath+" -Gcharset=UTF-8 -Nshape=plaintext -Nfontsize=10px -Tsvg");
	
	        BufferedOutputStream prIn = new BufferedOutputStream(pr.getOutputStream());
	        byte[] dotBytes = dot.toByteArray();
	        for(int i=0; i<dot.size(); i++)
	        	prIn.write(dotBytes[i]);
	        prIn.close();
	        
	        /*BufferedWriter in = new BufferedWriter(new OutputStreamWriter(pr.getOutputStream()));
	        in.write(dot);
	        in.flush();
	        in.close();*/
	        
	        response.setCharacterEncoding("UTF-8");
			PrintWriter out = response.getWriter();
	        
	        if (pr.getErrorStream().available()>0)
	        {
	        	response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
	        	response.setContentType("text/html");
	        	BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
	        	String line = null;
		        while((line=error.readLine()) != null) 
		        {
		        	out.write(line);
		        }
	        }
	        else
	        	response.setContentType("image/svg+xml");
		        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream(), "UTF-8"));
		        String line = null;
		        boolean svgStarted = false;
		        while((line=input.readLine()) != null) 
		        {
		        	if (mode!=null && mode.equals("snippet") && !svgStarted)
		        	{
		        		if (line.startsWith("<svg"))
		        			svgStarted=true;
		        		else
		        			continue;
		        	}
		        	else {
                        //Solve problem with font-size, measure "px" not specified, added
                        line = line.replaceAll("font-size:([\\d.]+);", "font-size:$1px;");
                        line = line.replaceAll("xlink:href=\"(.+)\"", "xlink:href=\""+request.getRequestURL()+"?rdf=$1\"");
                    }
		            out.write(line + "\n");
		        }
	        
	        out.flush();
	        out.close();
		}
		catch (Exception e)
		{
			throw new ServletException(e);
		}
	}

    private String getInput(HttpServletRequest request, String paramOrAttr) {
        return getInput(request, paramOrAttr, null);
    }

    private String getInput(HttpServletRequest request, String paramOrAttr, String defaultInput) {
        String input = (String)request.getSession().getAttribute(paramOrAttr);
        if (input==null)
            input = request.getParameter(paramOrAttr);
        if (input==null)
            input = defaultInput;
        return input;
    }

    private Model runInference(Model data, URL rules, int lineLength, int maxLineLength) throws IOException
	{	
		Reasoner reasoner = new GenericRuleReasoner(Rule.rulesFromURL(rules.toString()));
		InfModel inf = ModelFactory.createInfModel(reasoner, data);
		
		// Break long literals (more than lineLength chars) using carriage returns
		Model remove = ModelFactory.createDefaultModel();
		Model add = ModelFactory.createDefaultModel();
		Selector sel = new SimpleSelector(null, null, (String)null);
		for(StmtIterator sIt = inf.listStatements(sel); sIt.hasNext();)
		{
			Statement s = sIt.nextStatement();
			if(!s.getObject().isLiteral())
				continue;
			String l = s.getString();
			
			String lp = paginate(l, lineLength, maxLineLength);
			if (lp.length() != l.length())
			{
				remove.add(s);
				add.add(s.getSubject(), s.getPredicate(), lp, s.getLanguage());
			}
		}
		
		inf.remove(remove);
		inf.add(add);
		
		return inf;
	}
	
	private String paginate(String l, int lineLength, int maxLineLength) 
	{
		String lf = "\\n"; //"<BR/>" 
		if (l.length() > lineLength)
		{
			int pos = l.lastIndexOf(' ', lineLength);
			if (pos > 0)
				l = l.substring(0, pos) + lf + paginate(l.substring(pos+1, l.length()), lineLength, maxLineLength);
			else if (l.length() > maxLineLength)
				l = l.substring(0, maxLineLength) + lf + paginate(l.substring(maxLineLength, l.length()), lineLength, maxLineLength);
			else
				l = l.substring(0, lineLength) + paginate(l.substring(lineLength, l.length()), lineLength, maxLineLength);
		}	
		return l;
	}

	private ByteArrayOutputStream rdfModel2dot(Source transformation, Model graph) 
		throws TransformerException, IOException 
	{
		ByteArrayOutputStream result = new ByteArrayOutputStream();

		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer(transformation);
		
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		graph.write(o, "RDF/XML");
		o.flush();
		//String rdf = o.toString("UTF-8");

		ByteArrayInputStream rdf = new ByteArrayInputStream(o.toByteArray());
		
		if (language!=null) transformer.setParameter("language", language);
		if (namespaces!=null) transformer.setParameter("namespaces", namespaces);
		//StreamSource inStream = new StreamSource(new StringReader(rdf));
		StreamSource inStream = new StreamSource(rdf);
		StreamResult outStream = new StreamResult(result);
		transformer.transform(inStream, outStream);

		return result;
	}
}
