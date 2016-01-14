package com.superDaxue.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import com.superDaxue.controller.SchoolController;
import com.superDaxue.model.User;
import com.superDaxue.sql.UserSql;
import com.superDaxue.tool.AESTool;
import com.superDaxue.tool.MD5Tool;

public class LoginServlet extends HttpServlet {

	/**
	 * Constructor of the object.
	 */
	public LoginServlet() {
		super();
	}

	/**
	 * Destruction of the servlet. <br>
	 */
	public void destroy() {
		super.destroy(); // Just puts "destroy" string in log
		// Put your code here
	}

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}
	
	public void checkNum(HttpServletRequest request, HttpServletResponse response,SchoolController schoolController)
			throws ServletException, IOException {
		response.setContentType("text/html");
		response.setCharacterEncoding("utf-8");
		PrintWriter out = response.getWriter();
		String basePath = request.getSession().getServletContext().getRealPath(File.separator);
		String imgCode=schoolController.CheckNumUrl(basePath+"img"+File.separator+"checkNum"+File.separator);
		
		if(imgCode!=null){
			out.print("img/checkNum/checkNum"+imgCode+".jpg");
		}else {
			out.print("null");
		}
		out.flush();
		out.close();
		
		
	}
	
	/**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to post.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		String school=(String)request.getSession().getAttribute("school");
		if (school == null || "".equals(school)) {
			response.sendRedirect("../index.jsp");
			return;
		}
		SchoolController schoolController=(SchoolController)request.getSession().getAttribute("schoolController");
		if(schoolController==null){
			schoolController=new SchoolController(school);
		}
		String method=request.getParameter("method");
		if(method!=null&&!"".equals(method.trim())&&"checkNum".equals(method)){
			schoolController=new SchoolController(school);
			request.getSession().setAttribute("schoolController", schoolController);
			this.checkNum(request, response,schoolController);
			return;
		}
		String username=request.getParameter("username");
		String password=request.getParameter("password");
		String checkNum=request.getParameter("checkNum");
		String type=request.getParameter("type");
		User user=new User(username,password);
		JSONObject jsonObject = new JSONObject();

		jsonObject=schoolController.login(username, password,checkNum);
		if(jsonObject.get("result")!=null&&!"null".equals(jsonObject.get("result").toString())){
			UserSql userSql=new UserSql(school);
			AESTool aesTool=new AESTool();
			User user2=new User(aesTool.encrypt(username, "zhixin"),aesTool.encrypt(password, "zhixin"));//秘钥
			if(!userSql.insertUser(user2)){
				userSql.updateUser(user2);
			}
			int userid=userSql.getUserId(user2);
			if(userid==-1){
				response.sendRedirect("../"+school);
				return;
			}
			request.getSession().setAttribute("userid", userid);
			Cookie cookie=new Cookie("userCookie", userid+"");
			MD5Tool md5Tool=new MD5Tool();
			String value=md5Tool.GetMD5Code(md5Tool.GetMD5Code(user.getPassword()+school)+user.getUsername());
			Cookie token=new Cookie("token", value);
			Cookie schoolCookie=new Cookie("school", school);
			cookie.setPath("/");
			token.setPath("/");
			schoolCookie.setPath("/");
			cookie.setMaxAge(60 * 60 * 24 * 365 * 2);
			token.setMaxAge(60 * 60 * 24 * 365 * 2);
			schoolCookie.setMaxAge(60 * 60 * 24 * 365 * 2);
			response.addCookie(cookie);
			response.addCookie(token);
			response.addCookie(schoolCookie);
			jsonObject=schoolController.crawler(user.getUsername(), user.getPassword(), userid,checkNum);
		}
		request.setAttribute("resultJson", jsonObject);
		String path="";
		if(jsonObject.get("isSuccess")!=null&&"1".equals(jsonObject.get("isSuccess").toString())){
			path="TimetableServlet";
			if("1".equals(type)){
				path="ScoreServlet?success=1";
			}
			response.sendRedirect(path);
			return;
		}   
		else{
			path="../jsp/signin.jsp";
			if("1".equals(type)){
				path="ScoreServlet?success=2";
				response.sendRedirect(path);
				return;
			}
			RequestDispatcher dispatcher=request.getRequestDispatcher(path);
			dispatcher.forward(request, response);
		}
	}
	
	/**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException if an error occurs
	 */
	public void init() throws ServletException {
		// Put your code here
	}

	
}
