package org.powertac.tournament.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;


@WebFilter("/*")
public class CharacterEncodingFilter implements Filter
{
  @Override
  public void destroy ()
  {
    // TODO Auto-generated method
  }

  @Override
  public void doFilter (ServletRequest request,
                        ServletResponse response, FilterChain chain)
      throws ServletException, IOException
  {
    request.setCharacterEncoding("UTF-8");
    chain.doFilter(request, response);
  }

  @Override
  public void init (FilterConfig arg0) throws ServletException
  {
    // TODO Auto-generated method stub
  }
}
