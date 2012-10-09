/*
documentr - Edit, maintain, and present software documentation on the web.
Copyright (C) 2012 Maik Schreiber

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

var documentr = {};

(function() {
	"use strict";

	documentr.isSomething = function(x) {
		return (typeof(x) !== 'undefined') && (x !== null);
	};
	
	documentr.createPageTree = function(treeEl, options) {
		function getApplicationUrl() {
			return documentr.pageTreeOptions.applicationUrl;
		}
		
		function getProjectUrl(name) {
			return documentr.pageTreeOptions.projectUrl.replace(/_PROJECTNAME_/g, name);
		}
		
		function getBranchUrl(projectName, name) {
			return documentr.pageTreeOptions.branchUrl.replace(/_PROJECTNAME_/g, projectName).replace(/_BRANCHNAME_/g, name);
		}
		
		function getPageUrl(projectName, branchName, path) {
			return documentr.pageTreeOptions.pageUrl.replace(/_PROJECTNAME_/g, projectName).replace(/_BRANCHNAME_/g, branchName).replace(/_PAGEPATH_/g, path.replace(/\//g, ','));
		}
	
		if (!documentr.isSomething(options)) {
			options = {
				start: {
					type: 'application'
				}
			};
		}
		
		var startUrl;
		if (documentr.isSomething(options.start)) {
			if (options.start.type === 'project') {
				startUrl = getProjectUrl(options.start.projectName);
			} else if (options.start.type === 'branch') {
				startUrl = getBranchUrl(options.start.projectName, options.start.branchName);
			} else if (options.start.type === 'page') {
				startUrl = getPageUrl(options.start.projectName, options.start.branchName, options.start.pagePath);
			} else {
				startUrl = getApplicationUrl();
			}
		}

		var selectable = documentr.isSomething(options.selectable) ? options.selectable : {};
		var projectsSelectable = documentr.isSomething(selectable.projects) ? selectable.projects : true;
		var branchesSelectable = documentr.isSomething(selectable.branches) ? selectable.branches : true;
		var pagesSelectable = documentr.isSomething(selectable.pages) ? selectable.pages : true;
		
		var showPages = documentr.isSomething(options.showPages) ? options.showPages : true;
		var showAttachments = documentr.isSomething(options.showAttachments) ? options.showAttachments : false;
		
		var tree = treeEl.jstree({
			plugins: ['themes', 'json_data', 'ui', 'types'],
			core: {
				animation: 0
			},
			themes: {
				theme: 'documentr'
			},
			ui: {
				select_limit: 1
			},
			types: {
				max_children: -2,
				max_depth: -2,
				types: {
					project: {
						select_node: projectsSelectable,
						icon: {
							image: documentr.pageTreeOptions.iconUrls.project
						}
					},
					
					branch: {
						select_node: branchesSelectable,
						icon: {
							image: documentr.pageTreeOptions.iconUrls.branch
						}
					},
					
					page: {
						select_node: pagesSelectable,
						icon: {
							image: documentr.pageTreeOptions.iconUrls.page
						}
					},
					
					attachment: {
						icon: {
							image: documentr.pageTreeOptions.iconUrls.attachment
						}
					}
				}
			},
			json_data: {
				ajax: {
					url: function(node) {
						var url = null;
						if (node === -1) {
							url = startUrl;
						} else {
							node = $(node);
							var type = node.data('type');
							if (type === 'project') {
								url = getProjectUrl(node.data('name'));
							} else if (type === 'branch') {
								url = getBranchUrl(node.data('projectName'), node.data('name'));
							} else if (type === 'page') {
								url = getPageUrl(node.data('projectName'), node.data('branchName'), node.data('path'));
							}
						}
						return url;
					},
					data: function(node) {
						var d = {};
						if (documentr.isSomething(options.checkBranchPermissions)) {
							d.checkBranchPermissions = options.checkBranchPermissions;
						}
						d.pages = showPages;
						d.attachments = showAttachments;
						return d;
					},
					type: 'POST',
					dataType: 'json',
					success: function(nodes) {
						var treeNodes = [];
						for (var i = 0; i < nodes.length; i++) {
							var node = nodes[i];
							if (node.type === 'PROJECT') {
								treeNodes.push({
									data: documentr.pageTreeOptions.projectTitle.replace(/_PROJECTNAME_/g, node.name),
									attr: {
										rel: 'project'
									},
									metadata: {
										type: 'project',
										name: node.name
									},
									state: 'closed'
								});
							} else if (node.type === 'BRANCH') {
								treeNodes.push({
									data: documentr.pageTreeOptions.branchTitle.replace(/_BRANCHNAME_/g, node.name),
									attr: {
										rel: 'branch'
									},
									metadata: {
										type: 'branch',
										projectName: node.projectName,
										name: node.name
									},
									state: 'closed'
								});
							} else if (node.type === 'PAGE') {
								if (!documentr.isSomething(options.filterPage) ||
									((node.projectName + '/' + node.branchName + '/' + node.path.replace(/\//g, ',')) !==
										options.filterPage)) {
									
									treeNodes.push({
										data: node.title,
										attr: {
											rel: 'page'
										},
										metadata: {
											type: 'page',
											projectName: node.projectName,
											branchName: node.branchName,
											path: node.path,
											hasBranchPermissions: node.hasBranchPermissions
										},
										state: 'closed'
									});
								}
							} else if (node.type === 'ATTACHMENT') {
								treeNodes.push({
									data: node.name,
									attr: {
										rel: 'attachment'
									},
									metadata: {
										type: 'attachment',
										projectName: node.projectName,
										branchName: node.branchName,
										pagePath: node.pagePath,
										name: node.name
									}
								});
							}
						}
						return treeNodes;
					}
				}
			}
		})
		.delegate('a', 'click', function(event) {
			event.preventDefault();
		});
		return tree;
	};
	
	documentr.diffMarkdownAndGetHtml = function(markdown1, markdown2) {
		var dmp = new diff_match_patch();
		var diffs = dmp.diff_main(markdown1, markdown2);
		dmp.diff_cleanupSemantic(diffs);
	
		var html = [];
		for (var i = 0; i < diffs.length; i++) {
			var op = diffs[i][0];
			var text = diffs[i][1]
				.replace(/&/g, '&amp;')
				.replace(/</g, '&lt;')
				.replace(/>/g, '&gt;')
				.replace(/\n/g, '\n');
			switch (op) {
				case DIFF_INSERT:
					html[i] = '<ins>' + text + '</ins>';
					break;
				case DIFF_DELETE:
					html[i] = '<del>' + text + '</del>';
					break;
				case DIFF_EQUAL:
					html[i] = text;
					break;
			}
		}
		return '<pre class="changes"><code>' + html.join('') + '</code></pre>';
	};
	
	documentr.openMessageDialog = function(title, message, buttons, options) {
		var backdrop = (documentr.isSomething(options) && documentr.isSomething(options.backdrop)) ? options.backdrop : true;
		var keyboard = (documentr.isSomething(options) && documentr.isSomething(options.keyboard)) ? options.keyboard : true;
		var wide = (documentr.isSomething(options) && documentr.isSomething(options.wide)) ? options.wide : false;
		var messageAsHtml = (documentr.isSomething(options) && documentr.isSomething(options.messageAsHtml)) ? options.messageAsHtml : false;

		var id = "dialog_" + new Date().getTime();
		var html =
			'<div class="modal" id="' + id + '" style="display: none;">' +
				'<div class="modal-header">' +
					'<button class="close" id="' + id + '_close">&#x00D7</button>' +
					'<h3 id="' + id + '_title">title</h3>' +
				'</div>' +
				'<div class="modal-body" id="' + id + '_body"></div>' +
				'<div class="modal-footer" id="' + id + '_footer"></div>' +
			'</div>';
		var dlgEl = $(html);
		if (wide) {
			dlgEl.addClass('modal-wide');
		}
		
		var footerEl = dlgEl.find('#' + id + '_footer');

		function close() {
			dlgEl.hideModal();
			dlgEl.remove();
		}
		
		function setText(message) {
			if (messageAsHtml) {
				dlgEl.find('#' + id + '_body').html(message);
			} else {
				dlgEl.find('#' + id + '_body').text(message);
			}
		}
		
		function setAllButtonsDisabled() {
			footerEl.find('a').off('click').setButtonDisabled(true);
		}
		
		var dlg = {
			close: close,
			setText: setText,
			setAllButtonsDisabled: setAllButtonsDisabled
		};
		
		dlgEl.find('#' + id + '_title').text(title);
		setText(message);
		dlgEl.find('#' + id + '_close').click(close);
		var clickHandler = function(event) {
			var buttonOptions = event.data;
			var closeDlg = documentr.isSomething(buttonOptions.close) && buttonOptions.close;
			var cancelDlg = documentr.isSomething(buttonOptions.cancel) && buttonOptions.cancel;
			if (cancelDlg) {
				closeDlg = true;
			}
			
			if (closeDlg) {
				close();
			}
			if (!cancelDlg) {
				buttonOptions.onclick.call(dlg);
			}
			event.preventDefault();
		};
		for (var i = 0; i < buttons.length; i++) {
			var button = buttons[i];
			var b = $('<a href="#" class="btn"></a>');
			if (documentr.isSomething(button.onclick) ||
				(documentr.isSomething(button.cancel) && button.cancel)) {
				
				b.click(button, clickHandler);
			} else if (documentr.isSomething(button.href)) {
				b.attr('href', button.href);
			}
			b.text(button.text);
			if (documentr.isSomething(button.type)) {
				b.addClass('btn-' + button.type);
			}
			footerEl.append(b);
		}
		$(document.body).append(dlgEl);
		
		dlgEl.showModal({
			backdrop: backdrop,
			keyboard: keyboard
		});
		
		return dlg;
	};
	
	documentr.toggleSiteSearch = function() {
		var divEl = $('#site-search');
		divEl.slideToggle('fast', function() {
			if (divEl.css('display') !== 'none') {
				$('#site-search input').select();
			}
		});
	};
	
	documentr.fixTwitterCss = function() {
		var twitterCssEl = null;
		var documentrCssEl = null;
		$('link[rel="stylesheet"]').each(function() {
			var el = $(this);
			var href = el.attr('href');
			if (documentr.isSomething(href)) {
				if (href.indexOf('.twimg.com') > 0) {
					twitterCssEl = el;
				} else if (href.indexOf('/documentr.css') > 0) {
					documentrCssEl = el;
				}
			}
		});
		if ((twitterCssEl !== null) && (documentrCssEl !== null)) {
			twitterCssEl.detach();
			documentrCssEl.before(twitterCssEl);
		}
		$('.twtr-widget').removeAttr('id');
	};
	
	documentr.setupCodeViews = function() {
		$('.code-view-wrapper .code-view').each(function() {
			if (!$(this).hasClass('ace_editor')) {
				$(this).setupCodeView();
			}
		});
	};
	
	
	$.fn.extend({
		showModal: function(options) {
			var modalBackdrop = (documentr.isSomething(options) && documentr.isSomething(options.backdrop)) ? options.backdrop : true;
			var modalKeyboard = (documentr.isSomething(options) && documentr.isSomething(options.keyboard)) ? options.keyboard : true;
			this.modal({
				backdrop: modalBackdrop,
				keyboard: modalKeyboard
			});
			var win = $(window);
			this.css('margin-left', '0')
				.css('margin-top', '0')
				.css('left', Math.floor((win.width() - this.width()) / 2) + 'px')
				.css('top', Math.floor((win.height() - this.height()) / 2) + 'px');
			return this;
		},
		
		hideModal: function() {
			this.modal('hide');
			return this;
		},
		
		setPreventClick: function(preventClick) {
			var preventClickHandler = this.data('preventClickHandler');
			if (!documentr.isSomething(preventClickHandler) && preventClick) {
				preventClickHandler = function(event) {
					event.preventDefault();
				};
				this.data('preventClickHandler', preventClickHandler);
				this.click(preventClickHandler);
			} else if (documentr.isSomething(preventClickHandler) && !preventClick) {
				this.off('click', preventClickHandler);
				this.data('preventClickHandler', null);
			}
			return this;
		},
		
		setButtonDisabled: function(disabled) {
			if (disabled) {
				this.addClass('disabled');
			} else {
				this.removeClass('disabled');
			}
			this.setPreventClick(disabled);
			return this;
		},
		
		setupCodeView: function() {
			var editor = ace.edit(this[0]);
			editor.setTheme('ace/theme/chrome');
			var type = $(this).attr('data-type');
			if (documentr.isSomething(type)) {
				editor.session.setMode('ace/mode/' + type);
			}
			editor.setReadOnly(true);
			editor.setDisplayIndentGuides(true);
			editor.renderer.setShowGutter(true);
			editor.session.setUseWrapMode(false);
			editor.renderer.setShowPrintMargin(false);
			editor.session.setUseSoftTabs(false);
			editor.setHighlightSelectedWord(false);
			editor.setHighlightActiveLine(false);
			editor.renderer.hideCursor();
			var lines = editor.session.getDocument().getLength();
			lines = Math.min(lines, 20);
			this.parent().css('height', (lines * 20) + 'px');
			editor.resize();
			return this;
		}
	});
})();

$(function() {
	$.ajaxSetup({
		cache: false
	});

	documentr.setupCodeViews();

	$('#siteSearch input').bind('webkitspeechchange', function() {
		$('#siteSearch')[0].submit();
	});
});
