/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.integration.blackbox.api.v2.rc2;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.orcid.jaxb.model.common_rc2.Day;
import org.orcid.jaxb.model.common_rc2.FuzzyDate;
import org.orcid.jaxb.model.common_rc2.Month;
import org.orcid.jaxb.model.common_rc2.Title;
import org.orcid.jaxb.model.common_rc2.Url;
import org.orcid.jaxb.model.common_rc2.Visibility;
import org.orcid.jaxb.model.common_rc2.Year;
import org.orcid.jaxb.model.groupid_rc2.GroupIdRecord;
import org.orcid.jaxb.model.message.ScopePathType;
import org.orcid.jaxb.model.record.summary_rc2.ActivitiesSummary;
import org.orcid.jaxb.model.record.summary_rc2.EducationSummary;
import org.orcid.jaxb.model.record.summary_rc2.EmploymentSummary;
import org.orcid.jaxb.model.record.summary_rc2.FundingGroup;
import org.orcid.jaxb.model.record.summary_rc2.FundingSummary;
import org.orcid.jaxb.model.record.summary_rc2.PeerReviewGroup;
import org.orcid.jaxb.model.record.summary_rc2.PeerReviewSummary;
import org.orcid.jaxb.model.record.summary_rc2.WorkGroup;
import org.orcid.jaxb.model.record.summary_rc2.WorkSummary;
import org.orcid.jaxb.model.record_rc2.Education;
import org.orcid.jaxb.model.record_rc2.Employment;
import org.orcid.jaxb.model.record_rc2.Funding;
import org.orcid.jaxb.model.record_rc2.PeerReview;
import org.orcid.jaxb.model.record_rc2.Relationship;
import org.orcid.jaxb.model.record_rc2.Work;
import org.orcid.jaxb.model.record_rc2.ExternalID;
import org.orcid.jaxb.model.record_rc2.ExternalIDType;
import org.orcid.pojo.ajaxForm.PojoUtil;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.client.ClientResponse;

/**
 * 
 * @author Will Simpson
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-memberV2-context.xml" })
public class MemberV2Test extends BlackBoxBase {    

    protected static Map<String,String> accessTokens = new HashMap<String, String>();
    
    static List<GroupIdRecord> groupRecords = null;
    
    @Resource(name = "memberV2ApiClient_rc2")
    protected MemberV2ApiClientImpl memberV2ApiClient;
    
    @BeforeClass
    public static void beforeClass() {
        String clientId1 = System.getProperty("org.orcid.web.testClient1.clientId");        
        String clientId2 = System.getProperty("org.orcid.web.testClient2.clientId");
        if(PojoUtil.isEmpty(clientId2)) {
            revokeApplicationsAccess(clientId1);
        } else {
            revokeApplicationsAccess(clientId1, clientId2);
        }                
    }
    
    @AfterClass
    public static void afterClass() {
        String clientId1 = System.getProperty("org.orcid.web.testClient1.clientId");        
        String clientId2 = System.getProperty("org.orcid.web.testClient2.clientId");
        if(PojoUtil.isEmpty(clientId2)) {
            revokeApplicationsAccess(clientId1);
        } else {
            revokeApplicationsAccess(clientId1, clientId2);
        }
    }
    
    @Before
    public void before() throws JSONException, InterruptedException, URISyntaxException {
        cleanActivities();  
        groupRecords = createGroupIds();
    }

    @After
    public void after() throws JSONException, InterruptedException, URISyntaxException {
        cleanActivities();
    }    
    
    @Test
    public void testGetNotificationToken() throws JSONException, InterruptedException {
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        assertNotNull(accessToken);
    }

    @Test
    public void createViewUpdateAndDeleteWork() throws JSONException, InterruptedException, URISyntaxException {
        long time = System.currentTimeMillis();
        Work workToCreate = (Work) unmarshallFromPath("/record_2.0_rc2/samples/work-2.0_rc2.xml", Work.class);
        workToCreate.setPutCode(null);
        workToCreate.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID wExtId = new ExternalID();
        wExtId.setValue("Work Id " + time);
        wExtId.setType(ExternalIDType.AGR.name());
        wExtId.setRelationship(Relationship.PART_OF);
        workToCreate.getExternalIdentifiers().getExternalIdentifiers().add(wExtId);
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        ClientResponse postResponse = memberV2ApiClient.createWorkXml(user1OrcidId, workToCreate, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0_rc2/" + user1OrcidId + "/work/\\d+"));
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        Work gotWork = getResponse.getEntity(Work.class);
        assertEquals("common:title", gotWork.getWorkTitle().getTitle().getContent());
        gotWork.getWorkTitle().getTitle().setContent("updated title");
        ClientResponse putResponse = memberV2ApiClient.updateLocationXml(postResponse.getLocation(), accessToken, gotWork);
        assertEquals(Response.Status.OK.getStatusCode(), putResponse.getStatus());
        ClientResponse getAfterUpdateResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getAfterUpdateResponse.getStatus());
        Work gotAfterUpdateWork = getAfterUpdateResponse.getEntity(Work.class);
        assertEquals("updated title", gotAfterUpdateWork.getWorkTitle().getTitle().getContent());
        ClientResponse deleteResponse = memberV2ApiClient.deleteWorkXml(user1OrcidId, gotWork.getPutCode(), accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void testUpdateWorkWithProfileCreationTokenWhenClaimedAndNotSource() throws JSONException, InterruptedException, URISyntaxException {
        long time = System.currentTimeMillis();
        Work workToCreate = (Work) unmarshallFromPath("/record_2.0_rc2/samples/work-2.0_rc2.xml", Work.class);
        workToCreate.setPutCode(null);
        workToCreate.setVisibility(Visibility.PUBLIC);
        workToCreate.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID wExtId = new ExternalID();
        wExtId.setValue("Work Id " + time);
        wExtId.setType(ExternalIDType.AGR.name());
        wExtId.setRelationship(Relationship.SELF);
        workToCreate.getExternalIdentifiers().getExternalIdentifiers().add(wExtId);
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        ClientResponse postResponse = memberV2ApiClient.createWorkXml(user1OrcidId, workToCreate, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0_rc2/" + user1OrcidId + "/work/\\d+"));
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        Work gotWork = getResponse.getEntity(Work.class);
        assertEquals("common:title", gotWork.getWorkTitle().getTitle().getContent());
        gotWork.getWorkTitle().getTitle().setContent("updated title");
        String profileCreateToken = oauthHelper.getClientCredentialsAccessToken(client2ClientId, client2ClientSecret, ScopePathType.ORCID_PROFILE_CREATE);
        ClientResponse putResponse = memberV2ApiClient.updateLocationXml(postResponse.getLocation(), profileCreateToken, gotWork);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), putResponse.getStatus());
        ClientResponse getAfterUpdateResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getAfterUpdateResponse.getStatus());
        Work gotAfterUpdateWork = getAfterUpdateResponse.getEntity(Work.class);
        assertEquals("common:title", gotAfterUpdateWork.getWorkTitle().getTitle().getContent());
        ClientResponse deleteResponse = memberV2ApiClient.deleteWorkXml(user1OrcidId, gotWork.getPutCode(), accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void createViewUpdateAndDeleteEducation() throws JSONException, InterruptedException, URISyntaxException {
        Education education = (Education) unmarshallFromPath("/record_2.0_rc2/samples/education-2.0_rc2.xml", Education.class);
        education.setPutCode(null);
        education.setVisibility(Visibility.PUBLIC);
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        ClientResponse postResponse = memberV2ApiClient.createEducationXml(user1OrcidId, education, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0_rc2/" + user1OrcidId + "/education/\\d+"));
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        Education gotEducation = getResponse.getEntity(Education.class);
        assertEquals("education:department-name", gotEducation.getDepartmentName());
        assertEquals("education:role-title", gotEducation.getRoleTitle());
        gotEducation.setDepartmentName("updated dept. name");
        gotEducation.setRoleTitle("updated role title");
        ClientResponse putResponse = memberV2ApiClient.updateLocationXml(postResponse.getLocation(), accessToken, gotEducation);
        assertEquals(Response.Status.OK.getStatusCode(), putResponse.getStatus());
        ClientResponse getAfterUpdateResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getAfterUpdateResponse.getStatus());
        Education gotAfterUpdateEducation = getAfterUpdateResponse.getEntity(Education.class);
        assertEquals("updated dept. name", gotAfterUpdateEducation.getDepartmentName());
        assertEquals("updated role title", gotAfterUpdateEducation.getRoleTitle());
        ClientResponse deleteResponse = memberV2ApiClient.deleteEducationXml(user1OrcidId, gotEducation.getPutCode(), accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void testUpdateEducationWithProfileCreationTokenWhenClaimedAndNotSource() throws JSONException, InterruptedException, URISyntaxException {
        Education education = (Education) unmarshallFromPath("/record_2.0_rc2/samples/education-2.0_rc2.xml", Education.class);
        education.setPutCode(null);
        education.setVisibility(Visibility.PUBLIC);
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        ClientResponse postResponse = memberV2ApiClient.createEducationXml(user1OrcidId, education, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0_rc2/" + user1OrcidId + "/education/\\d+"));
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        Education gotEducation = getResponse.getEntity(Education.class);
        assertEquals("education:department-name", gotEducation.getDepartmentName());
        assertEquals("education:role-title", gotEducation.getRoleTitle());
        gotEducation.setDepartmentName("updated dept. name");
        gotEducation.setRoleTitle("updated role title");
        String profileCreateToken = oauthHelper.getClientCredentialsAccessToken(client2ClientId, client2ClientSecret, ScopePathType.ORCID_PROFILE_CREATE);
        ClientResponse putResponse = memberV2ApiClient.updateLocationXml(postResponse.getLocation(), profileCreateToken, gotEducation);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), putResponse.getStatus());
        ClientResponse getAfterUpdateResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getAfterUpdateResponse.getStatus());
        Education gotAfterUpdateEducation = getAfterUpdateResponse.getEntity(Education.class);
        assertEquals("education:department-name", gotAfterUpdateEducation.getDepartmentName());
        assertEquals("education:role-title", gotAfterUpdateEducation.getRoleTitle());
        ClientResponse deleteResponse = memberV2ApiClient.deleteEducationXml(user1OrcidId, gotEducation.getPutCode(), accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void createViewUpdateAndDeleteEmployment() throws JSONException, InterruptedException, URISyntaxException {
        Employment employment = (Employment) unmarshallFromPath("/record_2.0_rc2/samples/employment-2.0_rc2.xml", Employment.class);
        employment.setPutCode(null);
        employment.setVisibility(Visibility.PUBLIC);
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        ClientResponse postResponse = memberV2ApiClient.createEmploymentXml(user1OrcidId, employment, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0_rc2/" + user1OrcidId + "/employment/\\d+"));
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        Employment gotEmployment = getResponse.getEntity(Employment.class);
        assertEquals("affiliation:department-name", gotEmployment.getDepartmentName());
        assertEquals("affiliation:role-title", gotEmployment.getRoleTitle());
        gotEmployment.setDepartmentName("updated dept. name");
        gotEmployment.setRoleTitle("updated role title");
        ClientResponse putResponse = memberV2ApiClient.updateLocationXml(postResponse.getLocation(), accessToken, gotEmployment);
        assertEquals(Response.Status.OK.getStatusCode(), putResponse.getStatus());
        ClientResponse getAfterUpdateResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getAfterUpdateResponse.getStatus());
        Employment gotAfterUpdateEmployment = getAfterUpdateResponse.getEntity(Employment.class);
        assertEquals("updated dept. name", gotAfterUpdateEmployment.getDepartmentName());
        assertEquals("updated role title", gotAfterUpdateEmployment.getRoleTitle());
        ClientResponse deleteResponse = memberV2ApiClient.deleteEmploymentXml(user1OrcidId, gotEmployment.getPutCode(), accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void testUpdateEmploymentWithProfileCreationTokenWhenClaimedAndNotSource() throws JSONException, InterruptedException, URISyntaxException {
        Employment employment = (Employment) unmarshallFromPath("/record_2.0_rc2/samples/employment-2.0_rc2.xml", Employment.class);
        employment.setPutCode(null);
        employment.setVisibility(Visibility.PUBLIC);
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        ClientResponse postResponse = memberV2ApiClient.createEmploymentXml(user1OrcidId, employment, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0_rc2/" + user1OrcidId + "/employment/\\d+"));
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        Employment gotEmployment = getResponse.getEntity(Employment.class);
        assertEquals("affiliation:department-name", gotEmployment.getDepartmentName());
        assertEquals("affiliation:role-title", gotEmployment.getRoleTitle());
        gotEmployment.setDepartmentName("updated dept. name");
        gotEmployment.setRoleTitle("updated role title");
        String profileCreateToken = oauthHelper.getClientCredentialsAccessToken(client2ClientId, client2ClientSecret, ScopePathType.ORCID_PROFILE_CREATE);
        ClientResponse putResponse = memberV2ApiClient.updateLocationXml(postResponse.getLocation(), profileCreateToken, gotEmployment);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), putResponse.getStatus());
        ClientResponse getAfterUpdateResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getAfterUpdateResponse.getStatus());
        Employment gotAfterUpdateEmployment = getAfterUpdateResponse.getEntity(Employment.class);
        assertEquals("affiliation:department-name", gotAfterUpdateEmployment.getDepartmentName());
        assertEquals("affiliation:role-title", gotAfterUpdateEmployment.getRoleTitle());
        ClientResponse deleteResponse = memberV2ApiClient.deleteEmploymentXml(user1OrcidId, gotEmployment.getPutCode(), accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void createViewUpdateAndDeleteFunding() throws JSONException, InterruptedException, URISyntaxException {
        long time = System.currentTimeMillis();
        Funding funding = (Funding) unmarshallFromPath("/record_2.0_rc2/samples/funding-2.0_rc2.xml", Funding.class);
        funding.setPutCode(null);
        funding.setVisibility(Visibility.PUBLIC);
        funding.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID fExtId = new ExternalID();
        fExtId.setType(ExternalIDType.GRANT_NUMBER.name());
        fExtId.setValue("Funding Id " + time);
        fExtId.setRelationship(Relationship.SELF);
        funding.getExternalIdentifiers().getExternalIdentifiers().add(fExtId);
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        ClientResponse postResponse = memberV2ApiClient.createFundingXml(user1OrcidId, funding, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0_rc2/" + user1OrcidId + "/funding/\\d+"));
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        Funding gotFunding = getResponse.getEntity(Funding.class);
        assertEquals("common:title", gotFunding.getTitle().getTitle().getContent());
        assertEquals("common:translated-title", gotFunding.getTitle().getTranslatedTitle().getContent());
        assertEquals("en", gotFunding.getTitle().getTranslatedTitle().getLanguageCode());
        gotFunding.getTitle().getTitle().setContent("Updated title");
        gotFunding.getTitle().getTranslatedTitle().setContent("Updated translated title");
        gotFunding.getTitle().getTranslatedTitle().setLanguageCode("es");
        ClientResponse putResponse = memberV2ApiClient.updateLocationXml(postResponse.getLocation(), accessToken, gotFunding);
        assertEquals(Response.Status.OK.getStatusCode(), putResponse.getStatus());
        ClientResponse getAfterUpdateResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getAfterUpdateResponse.getStatus());
        Funding gotAfterUpdateFunding = getAfterUpdateResponse.getEntity(Funding.class);
        assertEquals("Updated title", gotAfterUpdateFunding.getTitle().getTitle().getContent());
        assertEquals("Updated translated title", gotAfterUpdateFunding.getTitle().getTranslatedTitle().getContent());
        assertEquals("es", gotAfterUpdateFunding.getTitle().getTranslatedTitle().getLanguageCode());
        ClientResponse deleteResponse = memberV2ApiClient.deleteFundingXml(user1OrcidId, gotFunding.getPutCode(), accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void testUpdateFundingWithProfileCreationTokenWhenClaimedAndNotSource() throws JSONException, InterruptedException, URISyntaxException {
        long time = System.currentTimeMillis();
        Funding funding = (Funding) unmarshallFromPath("/record_2.0_rc2/samples/funding-2.0_rc2.xml", Funding.class);
        funding.setPutCode(null);
        funding.setVisibility(Visibility.PUBLIC);
        funding.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID fExtId = new ExternalID();
        fExtId.setType(ExternalIDType.GRANT_NUMBER.name());
        fExtId.setValue("Funding Id " + time);
        fExtId.setRelationship(Relationship.SELF);
        funding.getExternalIdentifiers().getExternalIdentifiers().add(fExtId);
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        ClientResponse postResponse = memberV2ApiClient.createFundingXml(user1OrcidId, funding, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0_rc2/" + user1OrcidId + "/funding/\\d+"));
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        Funding gotFunding = getResponse.getEntity(Funding.class);
        assertEquals("common:title", gotFunding.getTitle().getTitle().getContent());
        assertEquals("common:translated-title", gotFunding.getTitle().getTranslatedTitle().getContent());
        assertEquals("en", gotFunding.getTitle().getTranslatedTitle().getLanguageCode());
        gotFunding.getTitle().getTitle().setContent("Updated title");
        gotFunding.getTitle().getTranslatedTitle().setContent("Updated translated title");
        gotFunding.getTitle().getTranslatedTitle().setLanguageCode("es");
        String profileCreateToken = oauthHelper.getClientCredentialsAccessToken(client2ClientId, client2ClientSecret, ScopePathType.ORCID_PROFILE_CREATE);
        ClientResponse putResponse = memberV2ApiClient.updateLocationXml(postResponse.getLocation(), profileCreateToken, gotFunding);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), putResponse.getStatus());
        ClientResponse getAfterUpdateResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getAfterUpdateResponse.getStatus());
        Funding gotAfterUpdateFunding = getAfterUpdateResponse.getEntity(Funding.class);
        assertEquals("common:title", gotAfterUpdateFunding.getTitle().getTitle().getContent());
        assertEquals("common:translated-title", gotAfterUpdateFunding.getTitle().getTranslatedTitle().getContent());
        assertEquals("en", gotAfterUpdateFunding.getTitle().getTranslatedTitle().getLanguageCode());
        ClientResponse deleteResponse = memberV2ApiClient.deleteFundingXml(user1OrcidId, gotFunding.getPutCode(), accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void createViewUpdateAndDeletePeerReview() throws JSONException, InterruptedException, URISyntaxException {
        long time = System.currentTimeMillis();
        PeerReview peerReviewToCreate = (PeerReview) unmarshallFromPath("/record_2.0_rc2/samples/peer-review-2.0_rc2.xml", PeerReview.class);
        peerReviewToCreate.setPutCode(null);
        peerReviewToCreate.setGroupId(groupRecords.get(0).getGroupId());
        peerReviewToCreate.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID wExtId = new ExternalID();
        wExtId.setValue("Work Id " + time);
        wExtId.setType(ExternalIDType.AGR.name());
        wExtId.setRelationship(Relationship.SELF);
        peerReviewToCreate.getExternalIdentifiers().getExternalIdentifiers().add(wExtId);
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);

        ClientResponse postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReviewToCreate, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0_rc2/" + user1OrcidId + "/peer-review/\\d+"));
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        PeerReview gotPeerReview = getResponse.getEntity(PeerReview.class);
        assertEquals("peer-review:url", gotPeerReview.getUrl().getValue());
        assertEquals("peer-review:subject-name", gotPeerReview.getSubjectName().getTitle().getContent());
        assertEquals(groupRecords.get(0).getGroupId(), gotPeerReview.getGroupId());
        gotPeerReview.getSubjectName().getTitle().setContent("updated title");

        ClientResponse putResponse = memberV2ApiClient.updateLocationXml(postResponse.getLocation(), accessToken, gotPeerReview);
        assertEquals(Response.Status.OK.getStatusCode(), putResponse.getStatus());
        ClientResponse getAfterUpdateResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getAfterUpdateResponse.getStatus());

        PeerReview gotAfterUpdateWork = getAfterUpdateResponse.getEntity(PeerReview.class);
        assertEquals("updated title", gotAfterUpdateWork.getSubjectName().getTitle().getContent());
        assertEquals(groupRecords.get(0).getGroupId(), gotAfterUpdateWork.getGroupId());
        ClientResponse deleteResponse = memberV2ApiClient.deletePeerReviewXml(user1OrcidId, gotAfterUpdateWork.getPutCode(), accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void testUpdatePeerReviewWithProfileCreationTokenWhenClaimedAndNotSource() throws JSONException, InterruptedException, URISyntaxException {
        long time = System.currentTimeMillis();
        PeerReview peerReviewToCreate = (PeerReview) unmarshallFromPath("/record_2.0_rc2/samples/peer-review-2.0_rc2.xml", PeerReview.class);
        peerReviewToCreate.setPutCode(null);
        peerReviewToCreate.setGroupId(groupRecords.get(0).getGroupId());
        peerReviewToCreate.setVisibility(Visibility.PUBLIC);
        peerReviewToCreate.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID wExtId = new ExternalID();
        wExtId.setValue("Work Id " + time);
        wExtId.setType(ExternalIDType.AGR.name());
        wExtId.setRelationship(Relationship.SELF);
        peerReviewToCreate.getExternalIdentifiers().getExternalIdentifiers().add(wExtId);
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);

        ClientResponse postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReviewToCreate, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0_rc2/" + user1OrcidId + "/peer-review/\\d+"));
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        PeerReview gotPeerReview = getResponse.getEntity(PeerReview.class);
        assertEquals("peer-review:subject-name", gotPeerReview.getSubjectName().getTitle().getContent());
        gotPeerReview.getSubjectName().getTitle().setContent("updated title");
        String profileCreateToken = oauthHelper.getClientCredentialsAccessToken(client2ClientId, client2ClientSecret, ScopePathType.ORCID_PROFILE_CREATE);
        ClientResponse putResponse = memberV2ApiClient.updateLocationXml(postResponse.getLocation(), profileCreateToken, gotPeerReview);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), putResponse.getStatus());
        ClientResponse getAfterUpdateResponse = memberV2ApiClient.viewLocationXml(postResponse.getLocation(), accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getAfterUpdateResponse.getStatus());
        PeerReview gotAfterUpdatePeerReview = getAfterUpdateResponse.getEntity(PeerReview.class);
        assertEquals("peer-review:subject-name", gotAfterUpdatePeerReview.getSubjectName().getTitle().getContent());
        ClientResponse deleteResponse = memberV2ApiClient.deletePeerReviewXml(user1OrcidId, gotAfterUpdatePeerReview.getPutCode(), accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    @Test
    public void testViewActivitiesSummaries() throws JSONException, InterruptedException, URISyntaxException {
        long time = System.currentTimeMillis();
        
        String accessTokenForClient1 = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        String accessTokenForClient2 = getAccessToken(this.client2ClientId, this.client2ClientSecret, this.client2RedirectUri);
        
        Education education = (Education) unmarshallFromPath("/record_2.0_rc2/samples/education-2.0_rc2.xml", Education.class);
        education.setPutCode(null);
        education.setVisibility(Visibility.PUBLIC);

        Employment employment = (Employment) unmarshallFromPath("/record_2.0_rc2/samples/employment-2.0_rc2.xml", Employment.class);
        employment.setPutCode(null);
        employment.setVisibility(Visibility.PUBLIC);

        Funding funding = (Funding) unmarshallFromPath("/record_2.0_rc2/samples/funding-2.0_rc2.xml", Funding.class);
        funding.setPutCode(null);
        funding.setVisibility(Visibility.PUBLIC);
        funding.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID fExtId = new ExternalID();
        fExtId.setType(ExternalIDType.GRANT_NUMBER.name());
        fExtId.setValue("Funding Id " + time);
        fExtId.setRelationship(Relationship.SELF);
        funding.getExternalIdentifiers().getExternalIdentifiers().add(fExtId);
                
        Work work = (Work) unmarshallFromPath("/record_2.0_rc2/samples/work-2.0_rc2.xml", Work.class);
        work.setPutCode(null);
        work.setVisibility(Visibility.PUBLIC);
        work.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID wExtId = new ExternalID();
        wExtId.setValue("Work Id " + time);
        wExtId.setType(ExternalIDType.AGR.name());
        wExtId.setRelationship(Relationship.SELF);
        work.getExternalIdentifiers().getExternalIdentifiers().add(wExtId);

        PeerReview peerReview = (PeerReview) unmarshallFromPath("/record_2.0_rc2/samples/peer-review-2.0_rc2.xml", PeerReview.class);
        peerReview.setPutCode(null);
        peerReview.setVisibility(Visibility.PUBLIC);
        peerReview.setGroupId(groupRecords.get(0).getGroupId());
        peerReview.getExternalIdentifiers().getExternalIdentifier().clear();        
        ExternalID pExtId = new ExternalID();
        pExtId.setValue("Work Id " + time);
        pExtId.setType(ExternalIDType.AGR.name());
        pExtId.setRelationship(Relationship.SELF);
        peerReview.getExternalIdentifiers().getExternalIdentifiers().add(pExtId);                

        ClientResponse postResponse = memberV2ApiClient.createEducationXml(user1OrcidId, education, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        postResponse = memberV2ApiClient.createEmploymentXml(user1OrcidId, employment, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        /**
         * Add 3 fundings 1 and 2 get grouped together 3 in another group
         * because it have different ext ids
         * **/

        // Add 1, the default funding
        postResponse = memberV2ApiClient.createFundingXml(user1OrcidId, funding, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        funding.getTitle().getTitle().setContent("Funding # 2");
        ExternalID fExtId3 = new ExternalID();
        fExtId3.setType(ExternalIDType.GRANT_NUMBER.name());
        fExtId3.setValue("extId3Value" + time);
        fExtId3.setRelationship(Relationship.SELF);
        funding.getExternalIdentifiers().getExternalIdentifiers().add(fExtId3);
        // Add 2, with the same ext ids +1
        postResponse = memberV2ApiClient.createFundingXml(user1OrcidId, funding, accessTokenForClient2);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        
        funding.getTitle().getTitle().setContent("Funding # 3");
        ExternalID fExtId4 = new ExternalID();
        fExtId4.setType(ExternalIDType.GRANT_NUMBER.name());
        fExtId4.setValue("extId4Value" + time);
        fExtId4.setRelationship(Relationship.SELF);
        funding.getExternalIdentifiers().getExternalIdentifiers().clear();
        funding.getExternalIdentifiers().getExternalIdentifiers().add(fExtId4);
        // Add 3, with different ext ids
        postResponse = memberV2ApiClient.createFundingXml(user1OrcidId, funding, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());        
        
        /**
         * Add 3 works 1 and 2 get grouped together 3 in another group because
         * it have different ext ids 
         **/
        // Add 1, the default work
        postResponse = memberV2ApiClient.createWorkXml(user1OrcidId, work, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        work.getWorkTitle().getTitle().setContent("Work # 2");
        ExternalID wExtId2 = new ExternalID();
        wExtId2.setType(ExternalIDType.DOI.name());
        wExtId2.setValue("doi-ext-id" + time);
        wExtId2.setRelationship(Relationship.SELF);
        work.getExternalIdentifiers().getExternalIdentifiers().add(wExtId2);
        // Add 2, with the same ext ids +1
        postResponse = memberV2ApiClient.createWorkXml(user1OrcidId, work, accessTokenForClient2);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        work.getWorkTitle().getTitle().setContent("Work # 3");
        ExternalID wExtId3 = new ExternalID();
        wExtId3.setType(ExternalIDType.EID.name());
        wExtId3.setValue("eid-ext-id" + time);
        wExtId3.setRelationship(Relationship.SELF);
        work.getWorkExternalIdentifiers().getExternalIdentifiers().clear();
        work.getWorkExternalIdentifiers().getExternalIdentifiers().add(wExtId3);
        // Add 3, with different ext ids
        postResponse = memberV2ApiClient.createWorkXml(user1OrcidId, work, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        /**
         * Add 4 peer reviews 1 and 2 get grouped together 3 in another group because
         * it have different group id 4 in another group because it doesnt have
         * any group id
         **/
        // Add 1, the default peer review
        postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReview, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        peerReview.setGroupId(groupRecords.get(0).getGroupId());
        peerReview.getSubjectName().getTitle().setContent("PeerReview # 2");
        peerReview.getCompletionDate().setDay(new Day(2));
        peerReview.getCompletionDate().setMonth(new Month(2));
        peerReview.getCompletionDate().setYear(new Year(2016));
        peerReview.setUrl(new Url("http://peer_review/2"));
        ExternalID pExtId2 = new ExternalID();
        pExtId2.setType(ExternalIDType.DOI.name());
        pExtId2.setValue("doi-ext-id" + System.currentTimeMillis());
        pExtId2.setRelationship(Relationship.SELF);
        
        for(ExternalID wei : peerReview.getExternalIdentifiers().getExternalIdentifiers()) {
            wei.setValue(wei.getValue()+ System.currentTimeMillis());
        }
        
        peerReview.getExternalIdentifiers().getExternalIdentifiers().add(pExtId2);
        // Add 2, with the same ext ids +1
        postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReview, accessTokenForClient2);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        peerReview.setGroupId(groupRecords.get(1).getGroupId());
        peerReview.getSubjectName().getTitle().setContent("PeerReview # 3");
        peerReview.getCompletionDate().setDay(new Day(3));
        peerReview.getCompletionDate().setMonth(new Month(3));
        peerReview.getCompletionDate().setYear(new Year(2017));
        peerReview.setUrl(new Url("http://peer_review/3"));
        ExternalID pExtId3 = new ExternalID();
        pExtId3.setType(ExternalIDType.EID.name());
        pExtId3.setValue("eid-ext-id" + System.currentTimeMillis());
        pExtId3.setRelationship(Relationship.SELF);
        peerReview.getExternalIdentifiers().getExternalIdentifiers().clear();
        peerReview.getExternalIdentifiers().getExternalIdentifiers().add(pExtId3);
        // Add 3, with different ext ids
        postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReview, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        peerReview.setGroupId(groupRecords.get(0).getGroupId());
        peerReview.getSubjectName().getTitle().setContent("PeerReview # 4");
        peerReview.getCompletionDate().setDay(new Day(4));
        peerReview.getCompletionDate().setMonth(new Month(4));
        peerReview.getCompletionDate().setYear(new Year(2018));
        peerReview.setUrl(new Url("http://peer_review/4"));
        
        ExternalID pExtId4 = new ExternalID();
        pExtId4.setType(ExternalIDType.EID.name());
        pExtId4.setValue("eid-ext-id" + System.currentTimeMillis());
        pExtId4.setRelationship(Relationship.SELF);
        peerReview.getExternalIdentifiers().getExternalIdentifiers().clear();
        peerReview.getExternalIdentifiers().getExternalIdentifiers().add(pExtId4);
        
        // Add 4, without ext ids
        postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReview, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        /**
         * Now, get the summaries and verify the following: - Education summary
         * is complete - Employment summary is complete - There are 3 groups of
         * fundings -- One group with 2 fundings -- One group with one funding
         * with ext ids -- One group with one funding without ext ids
         * 
         * - There are 3 groups of works -- One group with 2 works -- One group
         * with one work with ext ids -- One group with one work without ext ids
         * 
         * peer review -- There are 3 groups of peer reviews -- One group with 2 
         * peer reviews -- One groups with one peer review and ext ids -- One 
         * group with one peer review but without ext ids 
         **/

        ClientResponse activitiesResponse = memberV2ApiClient.viewActivities(user1OrcidId, accessTokenForClient1);
        assertEquals(Response.Status.OK.getStatusCode(), activitiesResponse.getStatus());
        ActivitiesSummary activities = activitiesResponse.getEntity(ActivitiesSummary.class);
        assertNotNull(activities);
        assertFalse(activities.getEducations().getSummaries().isEmpty());
        
        boolean found = false;
        for(EducationSummary summary : activities.getEducations().getSummaries()) {
            if(summary.getRoleTitle() != null && summary.getRoleTitle().equals("education:role-title")) {                
                assertEquals("education:department-name", summary.getDepartmentName());
                assertEquals(FuzzyDate.valueOf(1848, 2, 2), summary.getStartDate());
                assertEquals(FuzzyDate.valueOf(1848, 2, 2), summary.getEndDate());
                found = true;
                break;
            }
        }
        
        assertTrue("Education not found", found);
                
        assertFalse(activities.getEmployments().getSummaries().isEmpty());        
        found = false;
        for(EmploymentSummary summary : activities.getEmployments().getSummaries()) {
            if(summary.getRoleTitle() != null && summary.getRoleTitle().equals("affiliation:role-title")) {
                assertEquals("affiliation:department-name", summary.getDepartmentName());
                assertEquals(FuzzyDate.valueOf(1848, 2, 2), summary.getStartDate());
                assertEquals(FuzzyDate.valueOf(1848, 2, 2), summary.getEndDate());
                found = true;
                break;
            }
        }
        
        assertTrue("Employment not found", found);        
        
        assertNotNull(activities.getFundings());        
        boolean found1 = false, found2 = false, found3 = false, found4 = false;
        for (FundingGroup group : activities.getFundings().getFundingGroup()) {
            for(FundingSummary summary : group.getFundingSummary()) {
                if(summary.getTitle().getTitle().getContent().equals("common:title")) {
                    found1 = true;
                } else if(summary.getTitle().getTitle().getContent().equals("Funding # 2")) {
                    found2 = true;
                } else if(summary.getTitle().getTitle().getContent().equals("Funding # 3")) {
                    found3 = true;
                } 
            }
        }

        assertTrue("One of the fundings was not found: 1(" + found1 + ") 2(" + found2 + ") 3(" + found3 + ")", found1 == found2 == found3 == true);
        
        assertNotNull(activities.getWorks());        
        found1 = found2 = found3 = false;
        for (WorkGroup group : activities.getWorks().getWorkGroup()) {
            for(WorkSummary summary : group.getWorkSummary()) {
                if(summary.getTitle().getTitle().getContent().equals("common:title")) {
                    found1 = true;
                } else if(summary.getTitle().getTitle().getContent().equals("Work # 2")) {
                    found2 = true;
                } else if(summary.getTitle().getTitle().getContent().equals("Work # 3")) {
                    found3 = true;
                } else {
                    fail("Couldnt find work with title: " + summary.getTitle().getTitle().getContent());
                }
            }
        }
        
        assertTrue("One of the works was not found: 1(" + found1 + ") 2(" + found2 + ") 3(" + found3 + ")", found1 == found2 == found3 == true);
        
        assertNotNull(activities.getPeerReviews());        
        found1 = found2 = found3 = found4 = false;
        for(PeerReviewGroup group : activities.getPeerReviews().getPeerReviewGroup()) {
            for(PeerReviewSummary summary : group.getPeerReviewSummary()) {
                if(summary.getCompletionDate() != null && summary.getCompletionDate().getYear() != null) {
                    if(summary.getCompletionDate().getYear().getValue().equals("1848")) {
                        found1 = true;
                    } else if(summary.getCompletionDate().getYear().getValue().equals("2016")) {
                        found2 = true;
                    } else if(summary.getCompletionDate().getYear().getValue().equals("2017")) {
                        found3 = true;
                    } else if(summary.getCompletionDate().getYear().getValue().equals("2018")) {
                        found4 = true;
                    }
                }                               
            }
        }
        
        assertTrue("One of the peer reviews was not found: 1(" + found1 + ") 2(" + found2 + ") 3(" + found3 + ") 4(" + found4 + ")", found1 == found2 == found3 == found4 == true);        
    }
    
    @Test
    public void testPeerReviewMustHaveAtLeastOneExtId() throws JSONException, InterruptedException, URISyntaxException {
        PeerReview peerReview = (PeerReview) unmarshallFromPath("/record_2.0_rc2/samples/peer-review-2.0_rc2.xml", PeerReview.class);
        peerReview.setPutCode(null);
        peerReview.setGroupId(groupRecords.get(0).getGroupId());
        peerReview.getExternalIdentifiers().getExternalIdentifier().clear();        
        
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);

        ClientResponse postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReview, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResponse.getStatus());
    }    
    
    @Test
    public void testWorksWithPartOfRelationshipDontGetGrouped () throws JSONException, InterruptedException, URISyntaxException {
        long time = System.currentTimeMillis();
        String accessTokenForClient1 = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        String accessTokenForClient2 = getAccessToken(this.client2ClientId, this.client2ClientSecret, this.client2RedirectUri);
        
        Work work1 = (Work) unmarshallFromPath("/record_2.0_rc2/samples/work-2.0_rc2.xml", Work.class);
        work1.setPutCode(null);
        work1.setVisibility(Visibility.PUBLIC);
        work1.getExternalIdentifiers().getExternalIdentifier().clear();
        org.orcid.jaxb.model.record_rc2.WorkTitle title1 = new org.orcid.jaxb.model.record_rc2.WorkTitle();
        title1.setTitle(new Title("Work # 1" + time));
        work1.setWorkTitle(title1);
        ExternalID wExtId1 = new ExternalID();
        wExtId1.setValue("Work Id " + time);
        wExtId1.setType(ExternalIDType.AGR.name());
        wExtId1.setRelationship(Relationship.SELF);
        wExtId1.setUrl(new Url("http://orcid.org/work#1"));
        work1.getExternalIdentifiers().getExternalIdentifiers().clear();
        work1.getExternalIdentifiers().getExternalIdentifiers().add(wExtId1);

        Work work2 = (Work) unmarshallFromPath("/record_2.0_rc2/samples/work-2.0_rc2.xml", Work.class);
        work2.setPutCode(null);
        work2.setVisibility(Visibility.PUBLIC);
        org.orcid.jaxb.model.record_rc2.WorkTitle title2 = new org.orcid.jaxb.model.record_rc2.WorkTitle();
        title2.setTitle(new Title("Work # 2" + time));
        work2.setWorkTitle(title2);
        work2.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID wExtId2 = new ExternalID();
        wExtId2.setValue("Work Id " + time);
        wExtId2.setType(ExternalIDType.AGR.name());
        wExtId2.setRelationship(Relationship.PART_OF);
        wExtId2.setUrl(new Url("http://orcid.org/work#2"));
        work2.getExternalIdentifiers().getExternalIdentifiers().clear();
        work2.getExternalIdentifiers().getExternalIdentifiers().add(wExtId2);
        
        Work work3 = (Work) unmarshallFromPath("/record_2.0_rc2/samples/work-2.0_rc2.xml", Work.class);
        work3.setPutCode(null);
        work3.setVisibility(Visibility.PUBLIC);
        org.orcid.jaxb.model.record_rc2.WorkTitle title3 = new org.orcid.jaxb.model.record_rc2.WorkTitle();
        title3.setTitle(new Title("Work # 3" + time));
        work3.setWorkTitle(title3);        
        work3.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID wExtId3 = new ExternalID();
        wExtId3.setValue("Work Id " + time);
        wExtId3.setType(ExternalIDType.AGR.name());
        wExtId3.setRelationship(Relationship.SELF);
        wExtId3.setUrl(new Url("http://orcid.org/work#3"));
        work3.getExternalIdentifiers().getExternalIdentifiers().clear();
        work3.getExternalIdentifiers().getExternalIdentifiers().add(wExtId3);
        
        //Add the three works
        ClientResponse postResponse = memberV2ApiClient.createWorkXml(user1OrcidId, work1, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        postResponse = memberV2ApiClient.createWorkXml(user1OrcidId, work2, accessTokenForClient1);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        postResponse = memberV2ApiClient.createWorkXml(user1OrcidId, work3, accessTokenForClient2);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        
        ClientResponse activitiesResponse = memberV2ApiClient.viewActivities(user1OrcidId, accessTokenForClient1);
        assertEquals(Response.Status.OK.getStatusCode(), activitiesResponse.getStatus());
        ActivitiesSummary activities = activitiesResponse.getEntity(ActivitiesSummary.class);
        assertNotNull(activities);
        assertFalse(activities.getWorks().getWorkGroup().isEmpty());
        
        WorkGroup work1Group = null; 
        WorkGroup work2Group = null;
        WorkGroup work3Group = null;
        
        boolean work1found = false;
        boolean work2found = false;
        boolean work3found = false;
                                
        for(WorkGroup group : activities.getWorks().getWorkGroup()) {
            if(group.getIdentifiers().getExternalIdentifier() == null || group.getIdentifiers().getExternalIdentifier().isEmpty()) {
                for(WorkSummary summary : group.getWorkSummary()) {
                    String title = summary.getTitle().getTitle().getContent(); 
                    if (("Work # 2" + time).equals(title)) {
                        work2found = true;
                        work2Group = group;
                    }
                }
            } else {
                for(ExternalID id : group.getIdentifiers().getExternalIdentifiers()) {
                    //If it is the ID is the one we are looking for
                    if(id.getValue().equals("Work Id " + time)) {                    
                        for(WorkSummary summary : group.getWorkSummary()) {
                            String title = summary.getTitle().getTitle().getContent(); 
                            if(("Work # 1" + time).equals(title)) {
                                work1found = true;
                                work1Group = group;
                            } else if(("Work # 3" + time).equals(title)) {
                                work3found = true;
                                work3Group = group;
                            }
                        }
                    }
                }
            }            
        }
        
        assertTrue(work1found && work2found && work3found);
        //Check that work # 1 and Work # 3 are in the same work
        assertEquals(work1Group, work3Group);
        //Check that work # 2 is not in the same group than group # 1
        assertThat(work2Group, not(work1Group));
    }
    
    @Test
    public void testTokenWorksOnlyForTheScopeItWasIssued() throws JSONException, InterruptedException, URISyntaxException {
        long time = System.currentTimeMillis();
        String accessToken =  getAccessToken(ScopePathType.FUNDING_CREATE, this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);
        Work work1 = (Work) unmarshallFromPath("/record_2.0_rc2/samples/work-2.0_rc2.xml", Work.class);
        work1.setPutCode(null);
        work1.getExternalIdentifiers().getExternalIdentifier().clear();
        org.orcid.jaxb.model.record_rc2.WorkTitle title1 = new org.orcid.jaxb.model.record_rc2.WorkTitle();
        title1.setTitle(new Title("Work # 1"));
        work1.setWorkTitle(title1);
        ExternalID wExtId1 = new ExternalID();
        wExtId1.setValue("Work Id " + time);
        wExtId1.setType(ExternalIDType.AGR.name());
        wExtId1.setRelationship(Relationship.SELF);
        wExtId1.setUrl(new Url("http://orcid.org/work#1"));
        work1.getExternalIdentifiers().getExternalIdentifiers().clear();
        work1.getExternalIdentifiers().getExternalIdentifiers().add(wExtId1);
        
        
        //Add the work
        ClientResponse postResponse = memberV2ApiClient.createWorkXml(user1OrcidId, work1, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), postResponse.getStatus());
        
        Funding funding = (Funding) unmarshallFromPath("/record_2.0_rc2/samples/funding-2.0_rc2.xml", Funding.class);
        funding.setPutCode(null);
        funding.setVisibility(Visibility.PUBLIC);
        funding.getExternalIdentifiers().getExternalIdentifier().clear();
        ExternalID fExtId = new ExternalID();
        fExtId.setType(ExternalIDType.GRANT_NUMBER.name());
        fExtId.setValue("Funding Id " + time);
        fExtId.setRelationship(Relationship.SELF);
        funding.getExternalIdentifiers().getExternalIdentifiers().add(fExtId);
        
        //Add the funding
        postResponse = memberV2ApiClient.createFundingXml(user1OrcidId, funding, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
    }
    
    @Test
    public void testAddPeerReviewWithInvalidGroupingId() throws JSONException, InterruptedException, URISyntaxException {
        PeerReview peerReview = (PeerReview) unmarshallFromPath("/record_2.0_rc2/samples/peer-review-2.0_rc2.xml", PeerReview.class);
        peerReview.setPutCode(null);
        peerReview.setGroupId("Invalid group id " + System.currentTimeMillis());
        peerReview.getExternalIdentifiers().getExternalIdentifier().clear();        
        ExternalID pExtId = new ExternalID();
        pExtId.setValue("Work Id " + System.currentTimeMillis());
        pExtId.setType(ExternalIDType.AGR.name());
        pExtId.setRelationship(Relationship.SELF);
        peerReview.getExternalIdentifiers().getExternalIdentifiers().add(pExtId);
        
        String accessToken = getAccessToken(this.client1ClientId, this.client1ClientSecret, this.client1RedirectUri);

        //Pattern not valid
        ClientResponse postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReview, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResponse.getStatus());
        
        //Null group id 
        peerReview.setGroupId(null);
        postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReview, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResponse.getStatus());
        
        //Empty group id
        peerReview.setGroupId("");
        postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReview, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResponse.getStatus());
        
        //Invalid group id
        peerReview.setGroupId("orcid-generated:" + peerReview.getGroupId());
        postResponse = memberV2ApiClient.createPeerReviewXml(user1OrcidId, peerReview, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResponse.getStatus());        
    }        
    
    private void cleanActivities(String token) throws JSONException, InterruptedException, URISyntaxException {
        // Remove all activities        
        ClientResponse activitiesResponse = memberV2ApiClient.viewActivities(user1OrcidId, token);
        assertNotNull(activitiesResponse);
        ActivitiesSummary summary = activitiesResponse.getEntity(ActivitiesSummary.class);
        assertNotNull(summary);
        if (summary.getEducations() != null && !summary.getEducations().getSummaries().isEmpty()) {
            for (EducationSummary education : summary.getEducations().getSummaries()) {
                memberV2ApiClient.deleteEducationXml(user1OrcidId, education.getPutCode(), token);
            }
        }

        if (summary.getEmployments() != null && !summary.getEmployments().getSummaries().isEmpty()) {
            for (EmploymentSummary employment : summary.getEmployments().getSummaries()) {
                memberV2ApiClient.deleteEmploymentXml(user1OrcidId, employment.getPutCode(), token);
            }
        }

        if (summary.getFundings() != null && !summary.getFundings().getFundingGroup().isEmpty()) {
            for (FundingGroup group : summary.getFundings().getFundingGroup()) {
                for (FundingSummary funding : group.getFundingSummary()) {
                    memberV2ApiClient.deleteFundingXml(user1OrcidId, funding.getPutCode(), token);
                }
            }
        }

        if (summary.getWorks() != null && !summary.getWorks().getWorkGroup().isEmpty()) {
            for (WorkGroup group : summary.getWorks().getWorkGroup()) {
                for (WorkSummary work : group.getWorkSummary()) {
                    memberV2ApiClient.deleteWorkXml(user1OrcidId, work.getPutCode(), token);
                }
            }
        }
        
        if(summary.getPeerReviews() != null && !summary.getPeerReviews().getPeerReviewGroup().isEmpty()) {
            for(PeerReviewGroup group : summary.getPeerReviews().getPeerReviewGroup()) {
                for(PeerReviewSummary peerReview : group.getPeerReviewSummary()) {
                    memberV2ApiClient.deletePeerReviewXml(user1OrcidId, peerReview.getPutCode(), token);
                }
            }
        }
    }    
    
    public List<GroupIdRecord> createGroupIds() throws JSONException {
        //Use the existing ones
        if(groupRecords != null && !groupRecords.isEmpty()) 
            return groupRecords;
        
        List<GroupIdRecord> groups = new ArrayList<GroupIdRecord>();
        String token = oauthHelper.getClientCredentialsAccessToken(client1ClientId, client1ClientSecret, ScopePathType.GROUP_ID_RECORD_UPDATE);
        GroupIdRecord g1 = new GroupIdRecord();
        g1.setDescription("Description");
        g1.setGroupId("orcid-generated:01" + System.currentTimeMillis());
        g1.setName("Group # 1");
        g1.setType("publisher");
        
        GroupIdRecord g2 = new GroupIdRecord();
        g2.setDescription("Description");
        g2.setGroupId("orcid-generated:02" + System.currentTimeMillis());
        g2.setName("Group # 2");
        g2.setType("publisher");                
        
        ClientResponse r1 = memberV2ApiClient.createGroupIdRecord(g1, token); 
        
        String r1LocationPutCode = r1.getLocation().getPath().replace("/orcid-api-web/v2.0_rc2/group-id-record/", "");
        g1.setPutCode(Long.valueOf(r1LocationPutCode));
        groups.add(g1);
        
        ClientResponse r2 = memberV2ApiClient.createGroupIdRecord(g2, token);
        String r2LocationPutCode = r2.getLocation().getPath().replace("/orcid-api-web/v2.0_rc2/group-id-record/", "");
        g2.setPutCode(Long.valueOf(r2LocationPutCode));
        groups.add(g2);
        
        return groups;
    }
    
    public String getAccessToken(ScopePathType scope, String clientId, String clientSecret, String clientRedirectUri) throws InterruptedException, JSONException {
        String accessToken = super.getAccessToken(scope.value(), clientId, clientSecret, clientRedirectUri);
        return accessToken;
    }
    
    public String getAccessToken(String clientId, String clientSecret, String clientRedirectUri) throws InterruptedException, JSONException {        
        if(accessTokens.containsKey(clientId)) {
            return accessTokens.get(clientId);
        }
        
        String accessToken = super.getAccessToken(ScopePathType.ACTIVITIES_UPDATE.value() + " " + ScopePathType.ACTIVITIES_READ_LIMITED.value(), clientId, clientSecret, clientRedirectUri);        
        accessTokens.put(clientId,  accessToken);        
        return accessToken;
    }    

    public void cleanActivities() throws JSONException, InterruptedException, URISyntaxException {
        for(String token : accessTokens.values()) {
            cleanActivities(token);
        }
    }
}
