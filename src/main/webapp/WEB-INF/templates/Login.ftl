<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"> 
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en"> 
  <head> 
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/> 
    <title>Login</title> 
    <link rel="stylesheet" href="${contextPath}/VAADIN/themes/biopama/login_style.css" type="text/css" media="screen" charset="utf-8" /> 
    <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.2.6/jquery.min.js"></script> 
    <script type="text/javascript" src="${contextPath}/VAADIN/themes/biopama/js/openid-select.js">
    </script> 
    <script type="text/javascript"><!--//
    
    var loginReturnTo = '${loginReturnTo}';
    
    $(function() {
      $('#openid').openid({
        img_path: '${contextPath}/VAADIN/themes/biopama/img',
        txt: {
          label: 'Enter your {username} for <b>{provider}</b>',
          username: 'username',
          title: 'Select your Authentication method',
          sign: 'Go'
        }
      });
    });
    //--></script> 
  </head> 
  <body> 
    <div id="header">
    </div>
    <form method="get" action="login" id="openid">
    </form> 
    <div id="openid_authmessage">
      <#if authMessage??>
      	${authMessage}
      <#else>
        Please authenticate using an OpenID provider or ECAS.  If you are a first time user, your account will automatically be created, however will require activation before you can login.
        We suggest to use ECAS because the OpenID option will be removed in a near future.
      </#if>
        
    </div>
  </body> 
</html> 

