/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
import BlogPostItem from '@theme/BlogPostItem'
import BlogPostPaginator from '@theme/BlogPostPaginator'
import BlogSidebar from '@theme/BlogSidebar'
import { DiscussionEmbed } from 'disqus-react'
import EditThisPage from '@theme/EditThisPage'
import Layout from '@theme/Layout'
// eslint-disable-next-line no-use-before-define
import React from 'react'
import { ThemeClassNames } from '@docusaurus/theme-common'
import TOC from '@theme/TOC'

function BlogPostPage(props) {
	// eslint-disable-next-line react/prop-types
	const { content: BlogPostContents, sidebar } = props
	const { frontMatter, metadata } = BlogPostContents
	const { slug, title, description, nextItem, prevItem, editUrl } = metadata
	const { hide_table_of_contents: hideTableOfContents, no_comments } =
		frontMatter

	return (
		<Layout
			description={description}
			pageClassName={ThemeClassNames.page.blogPostPage}
			title={title}
			wrapperClassName={ThemeClassNames.wrapper.blogPages}
		>
			{BlogPostContents && (
				<div className='container margin-vert--lg'>
					<div className='row'>
						<div className='col col--3'>
							<BlogSidebar sidebar={sidebar} />
						</div>
						<main className='col col--7'>
							<BlogPostItem
								frontMatter={frontMatter}
								isBlogPostPage
								metadata={metadata}
							>
								<BlogPostContents />
							</BlogPostItem>
							<div>
								{editUrl && <EditThisPage editUrl={editUrl} />}
							</div>
							{!no_comments && (
								<DiscussionEmbed
									config={{
										identifier: slug,
										language: 'en_US',
										title: title,
										url: slug
									}}
									shortname='dassana'
								/>
							)}
							{(nextItem || prevItem) && (
								<div className='margin-vert--xl'>
									<BlogPostPaginator
										nextItem={nextItem}
										prevItem={prevItem}
									/>
								</div>
							)}
						</main>
						{!hideTableOfContents && BlogPostContents.toc && (
							<div className='col col--2'>
								<TOC toc={BlogPostContents.toc} />
							</div>
						)}
					</div>
				</div>
			)}
		</Layout>
	)
}

export default BlogPostPage
