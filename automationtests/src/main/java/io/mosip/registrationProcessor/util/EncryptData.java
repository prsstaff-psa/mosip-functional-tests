package io.mosip.registrationProcessor.util;

import java.io.File;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.mosip.dbdto.CryptomanagerDto;
import io.mosip.dbdto.DecrypterDto;
import io.mosip.dbdto.RegistrationPacketSyncDTO;
import io.mosip.dbdto.SyncRegistrationDto;
import io.mosip.kernel.core.util.DateUtils;

public class EncryptData {
	private String applicationId="REGISTRATION";
	ObjectMapper objectMapper=new ObjectMapper();

	@SuppressWarnings("unchecked")
	public JSONObject encryptData(RegistrationPacketSyncDTO registrationPacketSyncDto) {
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		String outputJson="";
		try {
			outputJson=objectMapper.writeValueAsString(registrationPacketSyncDto);
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		byte[] byteArray=outputJson.getBytes();
		String encryptedString=Base64.encodeBase64URLSafeString(byteArray);
		JSONObject encryptRequest=new JSONObject();
		CryptomanagerDto cryptoReq=new CryptomanagerDto();
		JSONObject cryptographicRequest=new JSONObject();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmssSSS");

		DecrypterDto decrypterDto=new DecrypterDto();

		String registrationId = registrationPacketSyncDto.getSyncRegistrationDTOs().get(0).getRegistrationId().toString();

		String referenceId=registrationId.substring(0,5)+"_"+registrationId.substring(5,10);

		decrypterDto.setApplicationId(applicationId);
		decrypterDto.setReferenceId(referenceId);
		decrypterDto.setData(encryptedString);
		if(registrationPacketSyncDto.getRequesttime()!=null && !(registrationPacketSyncDto.getRequesttime().matches(""))) {
			decrypterDto.setTimeStamp(getDateTimeFromString(registrationPacketSyncDto.getRequesttime()));
			cryptoReq.setRequesttime(getDateTimeFromString(registrationPacketSyncDto.getRequesttime()));
		}	else {
			decrypterDto.setTimeStamp(getDateTimeFromString("2019-03-02T06:29:41.011Z"));
			cryptoReq.setRequesttime(getDateTimeFromString("2019-03-02T06:29:41.011Z"));
			
		}
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		cryptographicRequest.put("applicationId", applicationId);
		cryptographicRequest.put("data", encryptedString);
		cryptographicRequest.put("referenceId", referenceId);
		if(decrypterDto.getTimeStamp()!=null && isValidTimestamp(decrypterDto.getTimeStamp().toString()+"Z")) {
			cryptographicRequest.put("timeStamp",decrypterDto.getTimeStamp().atOffset(ZoneOffset.UTC).toString());
		}else
			cryptographicRequest.put("timeStamp",getDateTimeFromString("2019-03-02T06:29:41.011Z"));
		
		encryptRequest.put("id","mosip.registration.sync");
		encryptRequest.put("metadata","");
		encryptRequest.put("request",cryptographicRequest);
		if(cryptoReq.getRequesttime()!=null && isValidTimestamp(cryptoReq.getRequesttime().toString()+"Z"))
			encryptRequest.put("requesttime", cryptoReq.getRequesttime().atOffset(ZoneOffset.UTC).toString());
		else
			encryptRequest.put("requesttime",getDateTimeFromString("2019-03-02T06:29:41.011Z"));
		encryptRequest.put("version","1.0");
		return encryptRequest;
	}

	public LocalDateTime getDateTimeFromString(String datetime) {
		 
			if(datetime==null || datetime.isEmpty() || datetime.equals("")) {
				return null;
			}else if(!isValidTimestamp(datetime)) {
				return null;
			}
		return DateUtils.convertUTCToLocalDateTime(datetime);
	}
	
	public boolean isValidTimestamp(String dateTime) {
		DateTimeFormatter formatters = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .parseStrict().toFormatter() ;
		try {
			LocalDate.parse(dateTime, formatters);
			return true;
		}catch(DateTimeParseException e) {
			return false;
		}
		
	}
	/*	public static void main(String[] args) {
	File f=new File("D:\\sprint_10\\mosip\\automationtests\\src\\test\\resources\\regProc\\Packets\\ValidPackets\\packteForInvalidPackets\\10011100110001920190514120310.zip");
	EncryptData e=new EncryptData();
	RegistrationPacketSyncDTO dto=e.createSyncRequest(f);
	System.out.println(dto.toString());
	System.out.println(e.encryptData(dto));
	}*/
	public RegistrationPacketSyncDTO createSyncRequest(File f, String regType) throws ParseException {
		RegProcApiRequests apiRequests = new RegProcApiRequests();
		
		String regId=f.getName().substring(0,f.getName().lastIndexOf("."));
		HashSequenceUtil hashSeqUtil = new HashSequenceUtil();
		String packetHash=hashSeqUtil.getPacketHashSequence(f);
		SyncRegistrationDto syncRegistrationDto=new SyncRegistrationDto();
		syncRegistrationDto.setLangCode("eng");
		syncRegistrationDto.setPacketHashValue(packetHash);
		syncRegistrationDto.setPacketSize(BigInteger.valueOf(f.length()));
		syncRegistrationDto.setRegistrationId(regId);
		syncRegistrationDto.setRegistrationType(regType);
		syncRegistrationDto.setSupervisorComment("APPROVED");
		syncRegistrationDto.setSupervisorStatus("APPROVED");
		List<SyncRegistrationDto> syncRegistrationList=new ArrayList<SyncRegistrationDto>();
		syncRegistrationList.add(syncRegistrationDto);
		RegistrationPacketSyncDTO registrationPacketSyncDto=new RegistrationPacketSyncDTO();
		registrationPacketSyncDto.setId("mosip.registration.sync");

		//LocalDateTime requestTime=LocalDateTime.ofInstant(currentDate.toInstant(), ZoneId.systemDefault());
		registrationPacketSyncDto.setRequesttime(apiRequests.getUTCTime().toString());
		registrationPacketSyncDto.setVersion("1.0");
		registrationPacketSyncDto.setSyncRegistrationDTOs(syncRegistrationList);
		return registrationPacketSyncDto;
	}
	public LocalDateTime getTime(String registrationId) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmssSSS");
		String packetCreatedDateTime = registrationId.substring(registrationId.length() - 14);
		int n = 100 + new Random().nextInt(900);
		String milliseconds = String.valueOf(n);

		Date date = formatter.parse(packetCreatedDateTime.substring(0, 8) + "T"
				+ packetCreatedDateTime.substring(packetCreatedDateTime.length() - 6)+milliseconds);
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

		return ldt;
	}

	public LocalDateTime getLocalDateTime(String time) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		String  responseTime = time;
		int n = 100 + new Random().nextInt(900);
		String milliseconds = String.valueOf(n);
		DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
		/*Date date = formatter1.parse(responseTime);
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());*/
		LocalDateTime dateTime = LocalDateTime.parse(responseTime.substring(0,responseTime.length()-1), formatter1);

		return dateTime;
	}

	public RegistrationPacketSyncDTO createSyncRequest(JSONObject jsonRequest) throws ParseException{
		String regId = null;
		String packetHash=null;
		long packetSize = 0;
		String langCode = null;
		String registrationType = null;
		String superVisiorStatus = null;
		JSONArray request = (JSONArray) jsonRequest.get("request");
		for(int j = 0; j<request.size() ; j++){
			JSONObject obj  = (JSONObject) request.get(j);
			regId = obj.get("registrationId").toString();
			packetHash = obj.get("packetHashValue").toString();
			packetSize =  (long) obj.get("packetSize");
			langCode = obj.get("langCode").toString();
			registrationType = obj.get("registrationType").toString();
			superVisiorStatus = obj.get("supervisorStatus").toString();
		}
		String id = jsonRequest.get("id").toString();
		String version = jsonRequest.get("version").toString();
		String requesttime = jsonRequest.get("requesttime").toString();

		SyncRegistrationDto syncRegistrationDto=new SyncRegistrationDto();
		syncRegistrationDto.setLangCode(langCode);
		syncRegistrationDto.setPacketHashValue(packetHash);
		syncRegistrationDto.setPacketSize(BigInteger.valueOf(packetSize));
		syncRegistrationDto.setRegistrationId(regId);
		syncRegistrationDto.setRegistrationType(registrationType);
		syncRegistrationDto.setSupervisorComment("APPROVED");
		syncRegistrationDto.setSupervisorStatus(superVisiorStatus);
		List<SyncRegistrationDto> syncRegistrationList=new ArrayList<SyncRegistrationDto>();
		syncRegistrationList.add(syncRegistrationDto);
		RegistrationPacketSyncDTO registrationPacketSyncDto=new RegistrationPacketSyncDTO();
		registrationPacketSyncDto.setId(id);
 
		//LocalDateTime requestTime=LocalDateTime.ofInstant(currentDate.toInstant(), ZoneId.systemDefault());
		//if(requesttime)
		registrationPacketSyncDto.setRequesttime(requesttime);
		registrationPacketSyncDto.setVersion(version);
		registrationPacketSyncDto.setSyncRegistrationDTOs(syncRegistrationList);
		return registrationPacketSyncDto;

	}
}
