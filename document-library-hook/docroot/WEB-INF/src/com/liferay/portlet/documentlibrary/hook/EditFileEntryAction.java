package com.liferay.portlet.documentlibrary.hook;

import com.liferay.portal.DuplicateLockException;
import com.liferay.portal.NoSuchRepositoryEntryException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.servlet.BrowserSnifferUtil;
import com.liferay.portal.kernel.servlet.ServletResponseConstants;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.PortletActionInvoker;
import com.liferay.portal.kernel.struts.StrutsPortletAction;
import com.liferay.portal.kernel.upload.UploadException;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.TempFileUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.PortletLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portlet.PortletURLFactoryUtil;
//import com.liferay.portlet.StrictPortletPreferencesImpl;
import com.liferay.portlet.asset.AssetCategoryException;
import com.liferay.portlet.asset.AssetTagException;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.assetpublisher.util.AssetPublisherUtil;
import com.liferay.portlet.documentlibrary.DuplicateFileException;
import com.liferay.portlet.documentlibrary.DuplicateFolderNameException;
import com.liferay.portlet.documentlibrary.FileExtensionException;
import com.liferay.portlet.documentlibrary.FileMimeTypeException;
import com.liferay.portlet.documentlibrary.FileNameException;
import com.liferay.portlet.documentlibrary.FileSizeException;
import com.liferay.portlet.documentlibrary.InvalidFileEntryTypeException;
import com.liferay.portlet.documentlibrary.InvalidFileVersionException;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.NoSuchFileVersionException;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.SourceFileNameException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.service.DLAppLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLAppServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.documentlibrary.util.DLUtil;
import com.liferay.portlet.dynamicdatamapping.StorageFieldRequiredException;
import com.liferay.portlet.trash.util.TrashUtil;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.MimeResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadBase.IOFileUploadException;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.process.Pipe;
import org.im4java.process.ProcessStarter;

import sun.misc.IOUtils;


public class EditFileEntryAction extends BaseStrutsPortletAction {

	public static final String TEMP_RANDOM_SUFFIX = "--tempRandomSuffix--";

	public void processAction( StrutsPortletAction originalStrutsPortletAction,
            PortletConfig portletConfig, ActionRequest actionRequest,
            ActionResponse actionResponse)
		throws Exception {
		
		String cmd = ParamUtil.getString(actionRequest, Constants.CMD);
	
		String propertiesImg = PrefsPropsUtil.getString("imagemagick.global.search.path");
		String[] splitePro = propertiesImg.split(";");
		String IMagicPath = splitePro[1]; //"C:\\Program Files\\ImageMagick";
		
		FileEntry fileEntry = null;
		
		UploadPortletRequest uploadPortletRequest =	PortalUtil.getUploadPortletRequest(actionRequest);		
		String contentType = StringPool.BLANK;
		if(cmd.equals(Constants.ADD)) {
			contentType = uploadPortletRequest.getContentType("file");
		}
		
		if(cmd.equals(Constants.ADD) && contentType.equalsIgnoreCase("image/jp2")){
	
			try {
				if (Validator.isNull(cmd)) {
					UploadException uploadException =
						(UploadException)actionRequest.getAttribute(
							WebKeys.UPLOAD_EXCEPTION);

					if (uploadException != null) {
						if (uploadException.isExceededSizeLimit()) {
							throw new FileSizeException(uploadException.getCause());
						}

						throw new PortalException(uploadException.getCause());
					}
				}
				else if (cmd.equals(Constants.ADD) ||
						 cmd.equals(Constants.ADD_DYNAMIC) ||
						 cmd.equals(Constants.UPDATE) ||
						 cmd.equals(Constants.UPDATE_AND_CHECKIN)) {
					
					fileEntry = updateFileEntry(
						portletConfig, actionRequest, actionResponse,IMagicPath);
				}
				
				WindowState windowState = actionRequest.getWindowState();
/*
				if (cmd.equals(Constants.ADD_TEMP) ||
					cmd.equals(Constants.DELETE_TEMP)) {

					setForward(actionRequest, ActionConstants.COMMON_NULL);
				}
				else if (cmd.equals(Constants.PREVIEW)) {
				}
				else if (!cmd.equals(Constants.MOVE_FROM_TRASH) &&
						 !windowState.equals(LiferayWindowState.POP_UP)) {

					sendRedirect(actionRequest, actionResponse);
				}
				else {*/
					String redirect = ParamUtil.getString(
						actionRequest, "redirect");
					int workflowAction = ParamUtil.getInteger(
						actionRequest, "workflowAction",
						WorkflowConstants.ACTION_SAVE_DRAFT);

					if ((fileEntry != null) &&
						(workflowAction == WorkflowConstants.ACTION_SAVE_DRAFT)) {

						redirect = getSaveAndContinueRedirect(portletConfig, actionRequest, fileEntry, redirect);

						sendRedirect(actionRequest, actionResponse, redirect);
					}
					else {
						if (!windowState.equals(LiferayWindowState.POP_UP)) {
							sendRedirect(actionRequest, actionResponse);
						}
						else {
							redirect = PortalUtil.escapeRedirect(
								ParamUtil.getString(actionRequest, "redirect"));

							if (Validator.isNotNull(redirect)) {
								if (cmd.equals(Constants.ADD) &&
									(fileEntry != null)) {

									String portletId = HttpUtil.getParameter(
										redirect, "p_p_id", false);

									String namespace =
										PortalUtil.getPortletNamespace(portletId);

									redirect = HttpUtil.addParameter(
										redirect, namespace + "className",
										DLFileEntry.class.getName());
									redirect = HttpUtil.addParameter(
										redirect, namespace + "classPK",
										fileEntry.getFileEntryId());
								}

								actionResponse.sendRedirect(redirect);
							}
						}
					}
				//}
			}
			catch (Exception e) {
				handleUploadException(portletConfig, actionRequest, actionResponse, cmd, e);
			}
			
		} else {
			 originalStrutsPortletAction.processAction(portletConfig, actionRequest, actionResponse);
		}

	}

	public String render(
            StrutsPortletAction originalStrutsPortletAction,
            PortletConfig portletConfig, RenderRequest renderRequest,
            RenderResponse renderResponse)
        throws Exception {
     
        return originalStrutsPortletAction.render(
            null, portletConfig, renderRequest, renderResponse);

    }

	public void serveResource(
            StrutsPortletAction originalStrutsPortletAction,
            PortletConfig portletConfig, ResourceRequest resourceRequest,
            ResourceResponse resourceResponse)
        throws Exception {
		
        originalStrutsPortletAction.serveResource(portletConfig, resourceRequest, resourceResponse);

    }

	protected FileEntry updateFileEntry(
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse,String IMagicPath)
		throws Exception {
		
		UploadPortletRequest uploadPortletRequest =
			PortalUtil.getUploadPortletRequest(actionRequest);

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		String cmd = ParamUtil.getString(uploadPortletRequest, Constants.CMD);

		long fileEntryId = ParamUtil.getLong(
			uploadPortletRequest, "fileEntryId");

		long repositoryId = ParamUtil.getLong(
			uploadPortletRequest, "repositoryId");
		long folderId = ParamUtil.getLong(uploadPortletRequest, "folderId");
		String sourceFileName = uploadPortletRequest.getFileName("file");
		String title = ParamUtil.getString(uploadPortletRequest, "title");
		String description = ParamUtil.getString(
			uploadPortletRequest, "description");
		String changeLog = ParamUtil.getString(
			uploadPortletRequest, "changeLog");
		boolean majorVersion = ParamUtil.getBoolean(
			uploadPortletRequest, "majorVersion");

		if (folderId > 0) {
			Folder folder = DLAppServiceUtil.getFolder(folderId);

			if (folder.getGroupId() != themeDisplay.getScopeGroupId()) {
				throw new NoSuchFolderException();
			}
		}

		InputStream inputStream = null;

		try {
			String contentType = uploadPortletRequest.getContentType("file");

			long size = uploadPortletRequest.getSize("file");

			if ((cmd.equals(Constants.ADD) ||
				 cmd.equals(Constants.ADD_DYNAMIC)) &&
				(size == 0)) {

				contentType = MimeTypesUtil.getContentType(title);
			}

			if (cmd.equals(Constants.ADD) ||
				cmd.equals(Constants.ADD_DYNAMIC) || (size > 0)) {

				String portletName = portletConfig.getPortletName();

				if (portletName.equals(PortletKeys.MEDIA_GALLERY_DISPLAY)) {
					PortletPreferences portletPreferences =	getStrictPortletSetup(actionRequest);

					if (portletPreferences == null) {
						portletPreferences = actionRequest.getPreferences();
					}

					String[] mimeTypes = DLUtil.getMediaGalleryMimeTypes(
						portletPreferences, actionRequest);

					if (Arrays.binarySearch(mimeTypes, contentType) < 0) {
						throw new FileMimeTypeException(contentType);
					}
				}
			}

			inputStream = uploadPortletRequest.getFileAsStream("file");

			ServiceContext serviceContext = ServiceContextFactory.getInstance(
				DLFileEntry.class.getName(), uploadPortletRequest);

			FileEntry fileEntry = null;

			if (cmd.equals(Constants.ADD) ||
				cmd.equals(Constants.ADD_DYNAMIC)) {

				// Hook Changes
				ProcessStarter.setGlobalSearchPath(IMagicPath); 
				DLFolder jp2Folder = DLFolderLocalServiceUtil.fetchFolder(themeDisplay.getScopeGroupId(), new Long(0), "JP2 FILE SOURCE");
				FileEntry jp2fileEntry = DLAppServiceUtil.addFileEntry(
								repositoryId, jp2Folder.getFolderId(), sourceFileName, contentType, title,
								description, changeLog, inputStream, size, serviceContext);
						
				AssetPublisherUtil.addAndStoreSelection(
						actionRequest, DLFileEntry.class.getName(),
						jp2fileEntry.getFileEntryId(), -1);
				
				String liferayHome = PropsUtil.get("liferay.home")+"/data/tmp/";
				String IMjp2Path = liferayHome+jp2fileEntry.getFileEntryId()+"jp.jp2"; 
				String IMjpgPath =liferayHome+jp2fileEntry.getFileEntryId()+".jpeg"; 
				File jp2File = new File(IMjp2Path);
				FileOutputStream jp2fos = new FileOutputStream(jp2File);
				if (!jp2File.exists()) {
					jp2File.createNewFile();
				}
				int bint = Integer.valueOf(String.valueOf(size));
				byte[] jp2Image=new byte[bint];//108,00,245
				inputStream.read(jp2Image);
				jp2fos.write(jp2Image,0,jp2Image.length);
				jp2fos.close();
				IMOperation op = new IMOperation();
				op.addImage(IMjp2Path);
				op.addImage(IMjpgPath);               // write to stdout in jpg-format
					
				File jpgFile = new File(IMjpgPath);
				jpgFile.createNewFile();
				FileOutputStream fos = new FileOutputStream(jpgFile);
				Pipe pipe = new Pipe(inputStream,fos);
				Pipe pipeIn  = new Pipe(inputStream,null);
				Pipe pipeOut = new Pipe(null,fos);

				// set up command
				ConvertCmd convert = new ConvertCmd();
				convert.setInputProvider(pipeIn);
				convert.setOutputConsumer(pipeOut);
				convert.run(op);
			    fos.close();
			    			    
				
			    File newJpgFile = new File(IMjpgPath);
//			    InputStream inputStreamJpg = new FileInputStream(newJpgFile);
						
				// Add file entry
			    
			    Map<String,Serializable> expandoBridgeAttributes = new HashMap<String, Serializable>();
			    expandoBridgeAttributes.put("jp2MimeType", "image/jp2");
			    expandoBridgeAttributes.put("jp2FileEntryId", String.valueOf(jp2fileEntry.getFileEntryId()));
			    serviceContext.setExpandoBridgeAttributes(expandoBridgeAttributes);
		    
			    fileEntry = DLAppServiceUtil.addFileEntry(repositoryId, folderId, newJpgFile.getName(),"image/jpeg", title, description, changeLog, newJpgFile, serviceContext);
				
			    System.out.println("Expando bridg ebtry id::=>"+fileEntry.getExpandoBridge().getAttribute("jp2FileEntryId")); 
			    
			    
				AssetPublisherUtil.addAndStoreSelection(
					actionRequest, DLFileEntry.class.getName(),
					fileEntry.getFileEntryId(), -1);
				
				newJpgFile.delete();
				jp2File.delete();
/*
				if (cmd.equals(Constants.ADD_DYNAMIC)) {
					JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

					jsonObject.put("fileEntryId", fileEntry.getFileEntryId());

					writeJSON(actionRequest, actionResponse, jsonObject);
				}
		*/		
			}
			else if (cmd.equals(Constants.UPDATE_AND_CHECKIN)) {

				// Update file entry and checkin

				fileEntry = DLAppServiceUtil.updateFileEntryAndCheckIn(
					fileEntryId, sourceFileName, contentType, title,
					description, changeLog, majorVersion, inputStream, size,
					serviceContext);
			}
			else {

				// Update file entry

				fileEntry = DLAppServiceUtil.updateFileEntry(
					fileEntryId, sourceFileName, contentType, title,
					description, changeLog, majorVersion, inputStream, size,
					serviceContext);
			}

			AssetPublisherUtil.addRecentFolderId(
				actionRequest, DLFileEntry.class.getName(), folderId);

			return fileEntry;
		}
		catch (Exception e) {
			UploadException uploadException =
				(UploadException)actionRequest.getAttribute(
					WebKeys.UPLOAD_EXCEPTION);

			if ((uploadException != null) &&
				uploadException.isExceededSizeLimit()) {

				throw new FileSizeException(uploadException.getCause());
			}

			throw e;
		}
		finally {
			StreamUtil.cleanUp(inputStream);
		}
	}

	protected String getSaveAndContinueRedirect(
			PortletConfig portletConfig, ActionRequest actionRequest,
			FileEntry fileEntry, String redirect)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

//		PortletURLImpl portletURL = new PortletURLImpl(
//			actionRequest, portletConfig.getPortletName(),
//			themeDisplay.getPlid(), PortletRequest.RENDER_PHASE);
		
		PortletURL portletURL = PortletURLFactoryUtil.create(actionRequest, portletConfig.getPortletName(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE);

		portletURL.setParameter(
			"struts_action", "/document_library/edit_file_entry");
//		portletURL.setParameter(Constants.CMD, Constants.UPDATE, false);
//		portletURL.setParameter("redirect", redirect, false);
		
		portletURL.setParameter(Constants.CMD, Constants.UPDATE);
		portletURL.setParameter("redirect", redirect);

		String referringPortletResource = ParamUtil.getString(
			actionRequest, "referringPortletResource");

//		portletURL.setParameter(
//			"referringPortletResource", referringPortletResource, false);
//
//		portletURL.setParameter(
//			"groupId", String.valueOf(fileEntry.getGroupId()), false);
//		portletURL.setParameter(
//			"fileEntryId", String.valueOf(fileEntry.getFileEntryId()), false);
//		portletURL.setParameter(
//			"version", String.valueOf(fileEntry.getVersion()), false);
		
		portletURL.setParameter(
				"referringPortletResource", referringPortletResource);

			portletURL.setParameter(
				"groupId", String.valueOf(fileEntry.getGroupId()));
			portletURL.setParameter(
				"fileEntryId", String.valueOf(fileEntry.getFileEntryId()));
			portletURL.setParameter(
				"version", String.valueOf(fileEntry.getVersion()));
		portletURL.setWindowState(actionRequest.getWindowState());

		return portletURL.toString();
	}

	protected void handleUploadException(
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse, String cmd, Exception e)
		throws Exception {

		if (e instanceof AssetCategoryException ||
			e instanceof AssetTagException) {

			SessionErrors.add(actionRequest, e.getClass(), e);
		}
		else if (e instanceof DuplicateFileException ||
				 e instanceof DuplicateFolderNameException ||
				 e instanceof FileExtensionException ||
				 e instanceof FileMimeTypeException ||
				 e instanceof FileNameException ||
				 e instanceof FileSizeException ||
				 e instanceof NoSuchFolderException ||
				 e instanceof SourceFileNameException ||
				 e instanceof StorageFieldRequiredException) {

			if (!cmd.equals(Constants.ADD_DYNAMIC) &&
				!cmd.equals(Constants.ADD_MULTIPLE) &&
				!cmd.equals(Constants.ADD_TEMP)) {

				SessionErrors.add(actionRequest, e.getClass());

				return;
			}

			if (e instanceof DuplicateFileException ||
				e instanceof FileExtensionException ||
				e instanceof FileNameException ||
				e instanceof FileSizeException) {

				HttpServletResponse response =
					PortalUtil.getHttpServletResponse(actionResponse);

				response.setContentType(ContentTypes.TEXT_HTML);
				response.setStatus(HttpServletResponse.SC_OK);

				String errorMessage = StringPool.BLANK;
				int errorType = 0;

				ThemeDisplay themeDisplay =
					(ThemeDisplay)actionRequest.getAttribute(
						WebKeys.THEME_DISPLAY);

				if (e instanceof DuplicateFileException) {
					errorMessage = themeDisplay.translate(
						"please-enter-a-unique-document-name");
					errorType =
						ServletResponseConstants.SC_DUPLICATE_FILE_EXCEPTION;
				}
				else if (e instanceof FileExtensionException) {
					errorMessage = themeDisplay.translate(
						"please-enter-a-file-with-a-valid-extension-x",
						StringUtil.merge(
							getAllowedFileExtensions(
								portletConfig, actionRequest, actionResponse)));
					errorType =
						ServletResponseConstants.SC_FILE_EXTENSION_EXCEPTION;
				}
				else if (e instanceof FileNameException) {
					errorMessage = themeDisplay.translate(
						"please-enter-a-file-with-a-valid-file-name");
					errorType = ServletResponseConstants.SC_FILE_NAME_EXCEPTION;
				}
				else if (e instanceof FileSizeException) {
					//long fileMaxSize = PrefsPropsUtil.getLong(
					//	PropsKeys.DL_FILE_MAX_SIZE);
					long fileMaxSize = PrefsPropsUtil.getLong(
							PropsKeys.DL_FILE_MAX_SIZE);

					if (fileMaxSize == 0) {
						fileMaxSize = PrefsPropsUtil.getLong(
							PropsKeys.UPLOAD_SERVLET_REQUEST_IMPL_MAX_SIZE);
					}

					fileMaxSize /= 1024;

					errorMessage = themeDisplay.translate(
						"please-enter-a-file-with-a-valid-file-size-no-larger" +
							"-than-x",
						fileMaxSize);

					errorType = ServletResponseConstants.SC_FILE_SIZE_EXCEPTION;
				}

				JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

				jsonObject.put("message", errorMessage);
				jsonObject.put("status", errorType);

				writeJSON(actionRequest, actionResponse, jsonObject);
			}

			SessionErrors.add(actionRequest, e.getClass());
		}
		else if (e instanceof DuplicateLockException ||
				 e instanceof InvalidFileVersionException ||
				 e instanceof NoSuchFileEntryException ||
				 e instanceof PrincipalException) {

			if (e instanceof DuplicateLockException) {
				DuplicateLockException dle = (DuplicateLockException)e;

				SessionErrors.add(actionRequest, dle.getClass(), dle.getLock());
			}
			else {
				SessionErrors.add(actionRequest, e.getClass());
			}

			setForward(actionRequest, "portlet.document_library.error");
		}
		else {
			throw e;
		}
	}
	protected String[] getAllowedFileExtensions(
			PortletConfig portletConfig, PortletRequest portletRequest,
			PortletResponse portletResponse)
		throws PortalException, SystemException {

		String portletName = portletConfig.getPortletName();

		if (!portletName.equals(PortletKeys.MEDIA_GALLERY_DISPLAY)) {
			return PrefsPropsUtil.getStringArray(
				PropsKeys.DL_FILE_EXTENSIONS, StringPool.COMMA);
		}
		else {
			PortletPreferences portletPreferences = getStrictPortletSetup(
				portletRequest);

			if (portletPreferences == null) {
				portletPreferences = portletRequest.getPreferences();
			}

			Set<String> extensions = new HashSet<String>();

			String[] mimeTypes = DLUtil.getMediaGalleryMimeTypes(
				portletPreferences, portletRequest);

			for (String mimeType : mimeTypes) {
				extensions.addAll(MimeTypesUtil.getExtensions(mimeType));
			}

			return extensions.toArray(new String[extensions.size()]);
		}
	}
	protected PortletPreferences getStrictPortletSetup(
			Layout layout, String portletId)
		throws PortalException, SystemException {

		if (Validator.isNull(portletId)) {
			return null;
		}

		PortletPreferences portletPreferences =
			PortletPreferencesFactoryUtil.getStrictPortletSetup(
				layout, portletId);

//		if (portletPreferences instanceof StrictPortletPreferencesImpl) {
//			throw new PrincipalException();
//		}

		return portletPreferences;
	}

	protected PortletPreferences getStrictPortletSetup(
			PortletRequest portletRequest)
		throws PortalException, SystemException {

		String portletResource = ParamUtil.getString(
			portletRequest, "portletResource");

		ThemeDisplay themeDisplay = (ThemeDisplay)portletRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		return getStrictPortletSetup(themeDisplay.getLayout(), portletResource);
	}
	
	protected void sendRedirect(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws IOException, SystemException {

		sendRedirect(actionRequest, actionResponse, null);
	}

	protected void sendRedirect(
			ActionRequest actionRequest, ActionResponse actionResponse,
			String redirect)
		throws IOException, SystemException {

		sendRedirect(null, actionRequest, actionResponse, redirect, null);
	}

	protected void sendRedirect(
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse, String redirect,
			String closeRedirect)
		throws IOException, SystemException {

		if (isDisplaySuccessMessage(actionRequest)) {
			addSuccessMessage(actionRequest, actionResponse);
		}

		if (Validator.isNull(redirect)) {
			redirect = (String)actionRequest.getAttribute(WebKeys.REDIRECT);
		}

		if (Validator.isNull(redirect)) {
			redirect = ParamUtil.getString(actionRequest, "redirect");
		}

		if ((portletConfig != null) && Validator.isNotNull(redirect) &&
			Validator.isNotNull(closeRedirect)) {

			redirect = HttpUtil.setParameter(
				redirect, "closeRedirect", closeRedirect);

			SessionMessages.add(
				actionRequest,
				PortalUtil.getPortletId(actionRequest) +
					SessionMessages.KEY_SUFFIX_CLOSE_REDIRECT,
				closeRedirect);
		}

		if (Validator.isNull(redirect)) {
			return;
		}

		// LPS-1928

		HttpServletRequest request = PortalUtil.getHttpServletRequest(
			actionRequest);

		if (BrowserSnifferUtil.isIe(request) &&
			(BrowserSnifferUtil.getMajorVersion(request) == 6.0) &&
			redirect.contains(StringPool.POUND)) {

			String redirectToken = "&#";

			if (!redirect.contains(StringPool.QUESTION)) {
				redirectToken = StringPool.QUESTION + redirectToken;
			}

			redirect = StringUtil.replace(
				redirect, StringPool.POUND, redirectToken);
		}

		redirect = PortalUtil.escapeRedirect(redirect);

		if (Validator.isNotNull(redirect)) {
			actionResponse.sendRedirect(redirect);
		}
	}

	protected void setForward(PortletRequest portletRequest, String forward) {
		portletRequest.setAttribute(getForwardKey(portletRequest), forward);
	}

	protected void writeJSON(
			PortletRequest portletRequest, ActionResponse actionResponse,
			Object json)
		throws IOException {

		HttpServletResponse response = PortalUtil.getHttpServletResponse(
			actionResponse);

		response.setContentType(ContentTypes.APPLICATION_JSON);

		ServletResponseUtil.write(response, json.toString());

		response.flushBuffer();
		
		//setForward(portletRequest, ActionConstants.COMMON_NULL);
		setForward(portletRequest, "/common/null.jsp");
	}

	protected void writeJSON(
			PortletRequest portletRequest, MimeResponse mimeResponse,
			Object json)
		throws IOException {

		mimeResponse.setContentType(ContentTypes.APPLICATION_JSON);

		PortletResponseUtil.write(mimeResponse, json.toString());

		mimeResponse.flushBuffer();
	}
	
	public static String getForwardKey(HttpServletRequest request) {
		String portletId = (String)request.getAttribute(WebKeys.PORTLET_ID);

		String portletNamespace = PortalUtil.getPortletNamespace(portletId);

		return portletNamespace.concat(WebKeys.PORTLET_STRUTS_FORWARD);
	}

	public static String getForwardKey(PortletRequest portletRequest) {
		String portletId = (String)portletRequest.getAttribute(
			WebKeys.PORTLET_ID);

		String portletNamespace = PortalUtil.getPortletNamespace(portletId);

		return portletNamespace.concat(WebKeys.PORTLET_STRUTS_FORWARD);
	}
	
	protected boolean isDisplaySuccessMessage(PortletRequest portletRequest)
			throws SystemException {

			if (!SessionErrors.isEmpty(portletRequest)) {
				return false;
			}

			ThemeDisplay themeDisplay = (ThemeDisplay)portletRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

			Layout layout = themeDisplay.getLayout();

			if (layout.isTypeControlPanel()) {
				return true;
			}

			String portletId = (String)portletRequest.getAttribute(
				WebKeys.PORTLET_ID);

			try {
				LayoutTypePortlet layoutTypePortlet =
					themeDisplay.getLayoutTypePortlet();

				if (layoutTypePortlet.hasPortletId(portletId)) {
					return true;
				}
			}
			catch (PortalException pe) {
				if (_log.isDebugEnabled()) {
					_log.debug(pe, pe);
				}
			}

			Portlet portlet = PortletLocalServiceUtil.getPortletById(
				themeDisplay.getCompanyId(), portletId);

			if (portlet.isAddDefaultResource()) {
				return true;
			}

			return false;
		}
	protected void addSuccessMessage(
			ActionRequest actionRequest, ActionResponse actionResponse) {

			PortletConfig portletConfig = (PortletConfig)actionRequest.getAttribute(
				JavaConstants.JAVAX_PORTLET_CONFIG);

			boolean addProcessActionSuccessMessage = GetterUtil.getBoolean(
				portletConfig.getInitParameter("add-process-action-success-action"),
				true);

			if (!addProcessActionSuccessMessage) {
				return;
			}

			String successMessage = ParamUtil.getString(
				actionRequest, "successMessage");

			SessionMessages.add(actionRequest, "requestProcessed", successMessage);
		}
	private static final String _TEMP_FOLDER_NAME =	EditFileEntryAction.class.getName();
	private static Log _log = LogFactoryUtil.getLog(EditFileEntryAction.class);

}