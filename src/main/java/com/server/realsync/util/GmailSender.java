package com.server.realsync.util;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.server.realsync.entity.Account;

@Service
public class GmailSender {

	@Autowired
    private TemplateEngine templateEngine;
	
	// Method to send email after photo detection
	public String getPhotoDetectedEmailContent(String guestName, String pin, String companyName) {
		return "<p>Dear " + guestName + ",</p>"
				+ "<p>Congrats! Your photo was detected by AiPixture. View your memory at <strong>" + pin
				+ "</strong>, " + "brought to you by <strong>" + companyName
				+ "</strong> and <strong>AiPixture</strong>.</p>"
				+ "<p>For any queries, contact us at <strong>+91-9606439784</strong> or visit "
				+ "<a href='https://www.aipixture.com'><strong>AiPixture</strong></a>.</p>" + "<p>Best regards,<br>"
				+ "The AiPixture Team</p>";
	}

	public String getAlbumSelectionEmailContent(String guestName, String eventName, String eventPhotoSelectionUrl) {
		return "<html>" + "<body>" + "Dear " + guestName + ",<br><br>" + "The photo upload for <strong>" + eventName
				+ "</strong> has been successfully completed. You can now proceed with selecting photos for the event album.<br><br>"
				+ "Please start the album selection by visiting the following link: <a href=\"" + eventPhotoSelectionUrl
				+ "\">" + eventPhotoSelectionUrl + "</a><br><br>"
				+ "Thank you for your time and attention. We look forward to your selections.<br><br>"
				+ "Best regards,<br>" + "The AiPixture Team<br>"
				+ "For any queries, contact us at <strong>+91-9606439784</strong> or visit <a href=\"https://www.aipixture.com\"><strong>AiPixture</strong></a>.<br>"
				+ "</body>" + "</html>";
	}

	public static void sendHTMLEmail(String recipient, String subject, String content) {
		// Sender's Gmail credentials
		final String senderEmail = "reachaipixture@gmail.com"; // Your Gmail address
		final String appPassword = "dsvn elaz qdrf sduc"; // Use App Password if 2FA is enabled

		// SMTP server configuration for Gmail
		Properties properties = new Properties();
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true"); // Enable STARTTLS
		properties.put("mail.smtp.host", "smtp.gmail.com"); // Gmail SMTP server
		properties.put("mail.smtp.port", "587"); // TLS Port
		properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");

		// Authenticate the sender's Gmail account
		Session session = Session.getInstance(properties, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(senderEmail, appPassword);
			}
		});

		try {
			// Compose the email
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(senderEmail));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
			message.setSubject(subject);
			message.setContent(content, "text/html"); // Email content

			// Send the email
			Transport.send(message);

			System.out.println("Email sent successfully! " + recipient);

		} catch (MessagingException e) {
			e.printStackTrace();
			System.out.println("Failed to send email: " + e.getMessage());
		}
	}

	public static void main(String[] args) {
		GmailSender gmailSender = new GmailSender();
		String recipientEmail = "palaniappan.lakshmanan1989@gmail.com"; // Recipient's email address
		gmailSender.sendCustRegisEmail(recipientEmail, "Pilliyar", "Muthu Studio");
		gmailSender.sendPhotoDetectedEmail(recipientEmail, "Pilliyar",
				"https://www.aipixture.com/6000/e/3029/guest?pin=9095", "Muthu Studio");
	}

	// Email for guest photo detected
	public void sendPhotoDetectedEmail(String recipientEmail, String guestName, String url, String companyName) {
		// Validate email before sending
		if (!isValidEmail(recipientEmail)) {
			System.out.println("Can't send email: Invalid email format: " + recipientEmail);
			return; // Exit method if email is invalid
		}
		String subject = "Your Photo Was Detected by AiPixture!";
		String content = getPhotoDetectedEmailContent(guestName, url, companyName);
		sendHTMLEmail(recipientEmail, subject, content);
	}

	public void sendAlbumSelectionEmail(String recipientEmail, String recipientName, String eventName,
			String selectionUrl) {
		// Validate email before sending
		if (!isValidEmail(recipientEmail)) {
			System.out.println("Can't send email: Invalid email format: " + recipientEmail);
			return; // Exit method if email is invalid
		}

		String subject = "Photo Upload Completed – Start Album Selection for " + eventName;
		String content = getAlbumSelectionEmailContent(recipientName, eventName, selectionUrl);
		sendHTMLEmail(recipientEmail, subject, content);
	}

	// Existing method to send the registration email
	public void sendCustRegisEmail(String recipientEmail, String custName, String companyName) {
		// Validate email before sending
		if (!isValidEmail(recipientEmail)) {
			System.out.println("Can't send email: Invalid email format: " + recipientEmail);
			return; // Exit method if email is invalid
		}
		String subject = "Welcome to AiPixture, " + custName + "!";
		String content = GmailSender.getCustRegisEmailContent(custName, companyName);
		sendHTMLEmail(recipientEmail, subject, content);
	}

	
	@Async
	public void bookingConfirmed(Account account, String eventDate, String name, String mobile) {
		
		Context context = new Context();
		context.setVariable("customerName", name);
		context.setVariable("customerMobile", mobile);
		context.setVariable("bookingDate", eventDate);
		context.setVariable("companyName", account.getName());
		
		// Load the HTML template as a String
		//String htmlContent = templateEngine.process("booking-request", context);
		
		//sendHTMLEmail(account.getEmail(), "Booking Request - "+name, htmlContent);

	}
	
	// Email validation regex pattern
	private static final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

	// Method to validate the email format
	public static boolean isValidEmail(String email) {
		Pattern pattern = Pattern.compile(EMAIL_REGEX);
		Matcher matcher = pattern.matcher(email);
		return matcher.matches();
	}

	// Existing registration email content method
	public static String getCustRegisEmailContent(String customerName, String companyName) {
		return "<!DOCTYPE html>" + "<html lang=\"en\">" + "<head>" + "<meta charset=\"UTF-8\">"
				+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
				+ "<title>Registration Confirmation</title>" + "<style>" + "body {"
				+ "    font-family: Arial, sans-serif;" + "    line-height: 1.6;" + "    margin: 0;"
				+ "    padding: 20px;" + "    background-color: #f4f4f4;" + "}" + ".container {"
				+ "    background-color: #ffffff;" + "    padding: 20px;" + "    border-radius: 5px;"
				+ "    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);" + "}" + "h1 {" + "    color: #333;" + "}" + "a {"
				+ "    color: #1a73e8;" + "    text-decoration: none;" + "}" + ".footer {" + "    margin-top: 20px;"
				+ "    font-size: 12px;" + "    color: #666;" + "}" + "</style>" + "</head>" + "<body>"
				+ "<div class=\"container\">" + "<h1>Dear " + customerName + ",</h1>"
				+ "<p>Welcome to <strong>AiPixture</strong>! &#x1F389;</p>"
				+ "<p>We are thrilled to have you onboard. Your registration is now complete, and you’re all set to experience the magic of AiPixture. We'll notify you as soon as your photo is matched - just sit back, relax, and stay tuned!</p>"
				+ "<p>If you have any questions or need assistance, feel free to reach out to us at <strong>+91-9606439784</strong> or visit our website at <strong><a href=\"https://www.aipixture.com\">AiPixture</a></strong>.</p>"
				+ "<p>Thank you for choosing <strong>" + companyName
				+ " by AiPixture</strong>. We're excited to bring your moments to life!</p>" + "<p>Warm regards,<br>"
				+ "The AiPixture Team<br>" + companyName + " by AiPixture</p>" + "</div>" + "<div class=\"footer\">"
				+ "<p>This email was sent to you because you registered with AiPixture. If you did not register, please ignore this email.</p>"
				+ "</div>" + "</body>" + "</html>";
	}
	
}
