<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${SUBJECT}</title>
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
</head>
<body style="margin: 0; padding: 0;">
    <table align="center" border="0" cellpadding="0" cellspacing="0"
        width="800" style="border-collapse: collapse;">
        <tr>
            <td><h2>${SUBJECT}</h2></td>
        </tr>
        <#if ORIGINAL??>
        <tr>
            <td>In response to your message:</td>
        </tr>
        <tr>
            <td><i>${ORIGINAL}</i></td>
        </tr>
        </#if>           
        <tr>
            <td bgcolor="#ffffff" style="padding: 20px 20px 20px 20px;">
                <table border="0" cellpadding="0" cellspacing="0" width="100%">
                    <tr>
                        <td>Hi ${TO},</td>
                    </tr>
                    <tr>
                        <td>you have new message from ${FROM} about <a href="#!${ESI_LINK}">${ESI_NAME}</a></td>
                    </tr>
                    <tr>
                        <td style="padding: 20px 0 0px 0;">Message:</td>
                    </tr>
                    <tr>
                        <td style="padding: 0px 0 20px 0;">${MESSAGE}</td>
                    </tr>
                </table>
        </tr>
    </table>
</body>
</html>