package io.jans.casa.plugins.authnmethod.service;

import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.OTPSmppExtension;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.stream.Stream;
import java.io.IOException;

/**
 * An app. scoped bean to serve the purpose of sending SMS using SMPP service
 * @author Stefan Andersson
 */
@ApplicationScoped
public class SmppMobilePhoneService extends MobilePhoneService {

    @Inject
    private Logger logger;

    private static final TimeFormatter TIME_FORMATTER = new AbsoluteTimeFormatter();
    private Boolean configured = false;
    private String smppServer;
    private Integer smppPort;

    private String systemId;
    private String password;

    private TypeOfNumber srcAddrTon;
    private NumberingPlanIndicator srcAddrNpi;
    private String srcAddr;

    private TypeOfNumber dstAddrTon;
    private NumberingPlanIndicator dstAddrNpi;

    private int priorityFlag;
    private Alphabet dataCodingAlphabet;
    private MessageClass dataCodinglMessageClass;

    @PostConstruct
    private void inited() {
        reloadConfiguration();
    }

    @Override
    public void reloadConfiguration() {

        configured = false;
        props = persistenceService.getCustScriptConfigProperties(OTPSmppExtension.ACR);

        if (props == null) {
            logger.warn("Config. properties for custom script '{}' could not be read. Features related to {} will not be accessible",
                    OTPSmppExtension.ACR, OTPSmppExtension.ACR.toUpperCase());
        } else {
            smppServer = props.get("smpp_server");
            String port = props.get("smpp_port");
            if (!port.isEmpty()) {
                smppPort = Integer.parseInt(port);
            }

            if (Stream.of(smppServer, port).anyMatch(Utils::isEmpty)) {
                logger.warn("Error parsing SMPP settings. Please check LDAP entry of SMPP custom script");
            } else {
                systemId = props.get("system_id");
                password = props.get("password");

                srcAddrTon = TypeOfNumber.valueOf(props.getOrDefault("source_addr_ton", "ALPHANUMERIC"));
                srcAddrNpi = NumberingPlanIndicator.valueOf(props.getOrDefault("source_addr_npi", "ISDN"));
                srcAddr = props.getOrDefault("source_addr", "Gluu OTP");

                dstAddrTon = TypeOfNumber.valueOf(props.getOrDefault("dest_addr_ton", "INTERNATIONAL"));
                dstAddrNpi = NumberingPlanIndicator.valueOf(props.getOrDefault("dest_addr_npi", "ISDN"));

                priorityFlag = 3;
                String priority_flag = props.get("priority_flag");
                if (priority_flag != null && !priority_flag.isEmpty()) {
                    priorityFlag = Integer.parseInt(priority_flag);
                }

                dataCodingAlphabet = Alphabet.valueOf(props.getOrDefault("data_coding_alphabet", "ALPHA_DEFAULT"));
                dataCodinglMessageClass = MessageClass.valueOf(props.getOrDefault("data_coding_message_class", "CLASS1"));
                configured = true;
            }
        }

    }

    @Override
    public SMSDeliveryStatus sendSMS(String number, String body) {

        SMSDeliveryStatus status;
        if (configured) {
            SMPPSession session = new SMPPSession();
            session.setTransactionTimer(10000);
            
            /**
             * We only handle international destination number reformatting.
             * All others may vary by configuration decisions taken on SMPP
             * server side which we have no clue about.
             */
            if (dstAddrTon == TypeOfNumber.INTERNATIONAL && number.charAt(0) == '+') {
                number = number.substring(1); // remove +
            }
            
            try {
                logger.info("Connecting to SMPP server {}", smppServer);
                String referenceId = session.connectAndBind(
                        smppServer,
                        smppPort,
                        new BindParameter(
                                BindType.BIND_TX,
                                systemId,
                                password,
                                null, // SystemType
                                srcAddrTon,
                                srcAddrNpi,
                                null
                        )
                );
                logger.info("Connected to SMPP server with system id {}", referenceId);

                try {
                    String messageId = session.submitShortMessage(
                            "CMT",
                            srcAddrTon,
                            srcAddrNpi,
                            srcAddr,
                            dstAddrTon,
                            dstAddrNpi,
                            number,
                            new ESMClass(),
                            (byte)0,                           // protocol id
                            (byte)priorityFlag,
                            TIME_FORMATTER.format(new Date()), // schedule delivery time
                            null,                              // validity period
                            new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                            (byte)0,                           // replace if present flag
                            new GeneralDataCoding(
                                    dataCodingAlphabet,
                                    dataCodinglMessageClass,
                                    false
                            ),
                            (byte)0,                           // default message id
                            body.getBytes()
                    ).getMessageId();

                    status = SMSDeliveryStatus.SUCCESS;
                    logger.info("Message \"{}\" sent to #{} with message id {}", body, number, messageId);
                } catch (PDUException e) {
                    status = SMSDeliveryStatus.DELIVERY_FAILED;
                    logger.error("Invalid PDU parameter: {}", e.getMessage());
                } catch (ResponseTimeoutException e) {
                    status = SMSDeliveryStatus.DELIVERY_FAILED;
                    logger.error("Response timeout: {}", e.getMessage());
                } catch (InvalidResponseException e) {
                    status = SMSDeliveryStatus.DELIVERY_FAILED;
                    logger.error("Receive invalid response: {}", e.getMessage());
                } catch (NegativeResponseException e) {
                    status = SMSDeliveryStatus.DELIVERY_FAILED;
                    logger.error("Receive negative response: {}", e.getMessage());
                } catch (IOException e) {
                    status = SMSDeliveryStatus.DELIVERY_FAILED;
                    logger.error("IO error occured: {}", e.getMessage());
                }

                session.unbindAndClose();
            } catch (IOException e) {
                status = SMSDeliveryStatus.PROVIDER_ERROR;
                logger.error("Failed connect and bind to host: {}", e.getMessage());
            }
        } else {
            status = SMSDeliveryStatus.APP_SETUP_ERROR;
            logger.info("No message was sent, SMPP settings was not initialized properly");
        }
        return status;

    }

}
